package com.example.launcherapp.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.launcherapp.data.repository.AccessControlRepositoryImpl
import com.example.launcherapp.domain.usecase.GetAllowedPackagesUseCase
import com.example.launcherapp.domain.usecase.GetAppUsageSnapshotsUseCase
import com.example.launcherapp.domain.usecase.GetLastUsageCheckUseCase
import com.example.launcherapp.domain.usecase.RecordAppUsageDeltaUseCase
import com.example.launcherapp.domain.usecase.ResetDailyUsageIfNeededUseCase
import com.example.launcherapp.domain.usecase.SetLastUsageCheckUseCase
import com.example.launcherapp.domain.usecase.UpdateAllowedPackagesUseCase
import com.example.launcherapp.data.usage.UsageAccessUtils
import java.util.concurrent.TimeUnit

class AppUsageMonitorWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val usageRepository = AppUsageRepositoryImpl(appContext)
    private val accessRepository = AccessControlRepositoryImpl(appContext)

    private val getAllowedPackages = GetAllowedPackagesUseCase(accessRepository)
    private val updateAllowedPackages = UpdateAllowedPackagesUseCase(accessRepository)
    private val getUsageSnapshots = GetAppUsageSnapshotsUseCase(usageRepository)
    private val recordUsageDelta = RecordAppUsageDeltaUseCase(usageRepository)
    private val resetDailyUsage = ResetDailyUsageIfNeededUseCase(usageRepository)
    private val getLastUsageCheck = GetLastUsageCheckUseCase(usageRepository)
    private val setLastUsageCheck = SetLastUsageCheckUseCase(usageRepository)

    override suspend fun doWork(): Result {
        if (!UsageAccessUtils.hasUsageAccess(applicationContext)) {
            return Result.success()
        }

        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        resetDailyUsage(now)

        val lastCheck = getLastUsageCheck().takeIf { it > 0L } ?: now - TimeUnit.MINUTES.toMillis(15)
        val events = usageStatsManager.queryEvents(lastCheck, now)
        val deltas = mutableMapOf<String, Long>()
        val foregroundStarts = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    foregroundStarts[pkg] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = foregroundStarts.remove(pkg)
                    if (start != null && event.timeStamp >= start) {
                        val delta = event.timeStamp - start
                        if (delta > 0) {
                            deltas[pkg] = (deltas[pkg] ?: 0L) + delta
                        }
                    }
                }
            }
        }
        foregroundStarts.forEach { (pkg, start) ->
            val delta = now - start
            if (delta > 0) {
                deltas[pkg] = (deltas[pkg] ?: 0L) + delta
            }
        }

        if (deltas.isNotEmpty()) {
            recordUsageDelta(deltas)
        }
        setLastUsageCheck(now)

        val allowedPackages = getAllowedPackages().toMutableSet()
        if (allowedPackages.isNotEmpty()) {
            val snapshots = getUsageSnapshots()
            var changed = false
            allowedPackages.toList().forEach { pkg ->
                val snapshot = snapshots[pkg]
                if (snapshot != null) {
                    val limit = snapshot.limitMinutes
                    val remaining = snapshot.remainingMinutes
                    if (limit != null && remaining != null && remaining <= 0) {
                        allowedPackages.remove(pkg)
                        changed = true
                    }
                }
            }
            if (changed) {
                updateAllowedPackages(allowedPackages)
            }
        }

        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "app_usage_monitor"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<AppUsageMonitorWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
        }

    }
}
