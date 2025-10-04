package com.example.launcherapp.data.usage

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.example.launcherapp.ActiveLauncherActivity
import com.example.launcherapp.data.repository.AccessControlRepositoryImpl
import com.example.launcherapp.domain.usecase.GetAllowedPackagesUseCase
import com.example.launcherapp.domain.usecase.GetAppUsageSnapshotsUseCase
import com.example.launcherapp.domain.usecase.RecordAppUsageDeltaUseCase
import com.example.launcherapp.domain.usecase.ResetDailyUsageIfNeededUseCase
import com.example.launcherapp.domain.usecase.UpdateAllowedPackagesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.jvm.Volatile

/**
 * Accessibility service that watches foreground-window changes so we can update usage
 * immediately and enforce limits in real time.
 *
 * When the user switches apps we:
 * 1. Close the previous foreground session, committing the elapsed time to storage.
 * 2. Start a real-time stopwatch for the new foreground package so we can react the
 *    moment its quota is exhausted.
 * 3. Re-check the quota and, if it is exhausted, remove the package from the allowed
 *    set and bounce the user back to Nebula Home.
 *
 * The periodic WorkManager job still runs as a backup to catch missed deltas (service
 * disabled, device idle, etc.) and handle daily resets.
 */
class ForegroundAppAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private val accessRepository by lazy { AccessControlRepositoryImpl(applicationContext) }
    private val usageRepository by lazy { AppUsageRepositoryImpl(applicationContext) }

    private val sessionMutex = Mutex()
    @Volatile private var activeSession: ActiveSession? = null
    private var tickerJob: Job? = null

    private val getAllowedPackages by lazy { GetAllowedPackagesUseCase(accessRepository) }
    private val getUsageSnapshots by lazy { GetAppUsageSnapshotsUseCase(usageRepository) }
    private val resetDailyUsage by lazy { ResetDailyUsageIfNeededUseCase(usageRepository) }
    private val recordUsageDelta by lazy { RecordAppUsageDeltaUseCase(usageRepository) }
    private val updateAllowedPackages by lazy { UpdateAllowedPackagesUseCase(accessRepository) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        Log.d(TAG, "event from $packageName")
        serviceScope.launch {
            handleForegroundChange(packageName)
        }
    }

    override fun onInterrupt() {
        // no-op
    }

    // Close any previous session, start a new one for the current foreground package,
    // and kick off the live ticker so limits can be enforced mid-session.
    private suspend fun handleForegroundChange(packageName: String) {
        resetDailyUsage(System.currentTimeMillis())
        val now = System.currentTimeMillis()

        // If we already have a live session for this package we can skip resetting it.
        val alreadyTracking = sessionMutex.withLock {
            activeSession?.packageName == packageName
        }
        if (alreadyTracking) {
            return
        }

        sessionMutex.withLock {
            finishActiveSessionLocked(now, cancelTicker = true)
        }

        val snapshots = getUsageSnapshots()
        val snapshot = snapshots[packageName]
        val baseUsage = snapshot?.usedMillisToday ?: 0L
        val limitMinutes = snapshot?.limitMinutes

        sessionMutex.withLock {
            val session = ActiveSession(packageName, now, baseUsage, limitMinutes)
            activeSession = session
            startTickerLocked(session)
        }

        enforceUsageLimitIfNeeded(
            packageName = packageName,
            liveUsageMillisOverride = baseUsage,
            limitMinutesOverride = limitMinutes
        )
    }

    // Must be invoked with the sessionMutex held.
    private suspend fun finishActiveSessionLocked(endTime: Long, cancelTicker: Boolean) {
        val session = activeSession ?: return
        if (cancelTicker) {
            tickerJob?.cancel()
            tickerJob = null
        }
        val elapsed = (endTime - session.startedAtMillis).coerceAtLeast(0L)
        if (elapsed > 0L) {
            Log.d(TAG, "Recording session of $elapsed ms for ${session.packageName}")
            recordUsageDelta(mapOf(session.packageName to elapsed))
        }
        activeSession = null
    }

    private fun startTickerLocked(session: ActiveSession) {
        tickerJob?.cancel()
        val job = serviceScope.launch {
            val currentJob = coroutineContext[Job]
            try {
                while (isActive) {
                    delay(TICK_INTERVAL_MILLIS)

                    val snapshot = sessionMutex.withLock { activeSession }
                    if (snapshot == null || snapshot.packageName != session.packageName) {
                        break
                    }

                    val limitMinutes = snapshot.limitMinutes ?: continue
                    val now = System.currentTimeMillis()
                    val elapsed = (now - snapshot.startedAtMillis).coerceAtLeast(0L)
                    val liveUsageMillis = snapshot.baseUsageMillis + elapsed
                    val limitMillis = limitMinutes * 60_000L

                    if (liveUsageMillis >= limitMillis) {
                        val usageAtLimit = sessionMutex.withLock {
                            val latest = activeSession
                            if (latest == null || latest.packageName != session.packageName) {
                                null
                            } else {
                                val finalNow = System.currentTimeMillis()
                                val finalElapsed = (finalNow - latest.startedAtMillis).coerceAtLeast(0L)
                                finishActiveSessionLocked(finalNow, cancelTicker = false)
                                latest.baseUsageMillis + finalElapsed
                            }
                        } ?: break

                        enforceUsageLimitIfNeeded(
                            packageName = session.packageName,
                            liveUsageMillisOverride = usageAtLimit,
                            limitMinutesOverride = limitMinutes
                        )
                        break
                    }
                }
            } finally {
                sessionMutex.withLock {
                    if (tickerJob === currentJob) {
                        tickerJob = null
                    }
                }
            }
        }
        tickerJob = job
    }

    private suspend fun enforceUsageLimitIfNeeded(
        packageName: String,
        liveUsageMillisOverride: Long? = null,
        limitMinutesOverride: Int? = null
    ) {
        Log.d(TAG, "Checking $packageName")

        val allowed = getAllowedPackages()
        if (!allowed.contains(packageName)) return

        val limitMinutes = limitMinutesOverride
            ?: getUsageSnapshots()[packageName]?.limitMinutes
        if (limitMinutes == null) return

        val usedMillis = liveUsageMillisOverride
            ?: getUsageSnapshots()[packageName]?.usedMillisToday
            ?: 0L
        val limitMillis = limitMinutes * 60_000L

        if (usedMillis >= limitMillis) {
            Log.d(TAG, "Limit hit for $packageName – launching guard.")
            val newAllowed = allowed.toMutableSet().apply { remove(packageName) }
            updateAllowedPackages(newAllowed)
            launchLockScreen()
        } else {
            Log.d(TAG, "Usage $usedMillis/$limitMillis for $packageName")
        }
    }

    private data class ActiveSession(
        val packageName: String,
        val startedAtMillis: Long,
        val baseUsageMillis: Long,
        val limitMinutes: Int?
    )

    private fun launchLockScreen() {
        val intent = Intent(this, ActiveLauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        ContextCompat.startActivity(this, intent, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    companion object {
        private const val TAG = "NebulaAccessibility"
        private const val TICK_INTERVAL_MILLIS = 1_000L

        fun createSettingsIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

        fun isEnabled(context: Context): Boolean {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            val targetId = context.packageName + "/" + ForegroundAppAccessibilityService::class.java.name
            return enabledServices.any { it.id.equals(targetId, ignoreCase = true) }
        }
    }
}
