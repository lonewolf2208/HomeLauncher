package com.example.launcherapp.data.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.launcherapp.data.repository.AccessControlRepositoryImpl
import com.example.launcherapp.domain.usecase.GetAllowedPackagesUseCase
import com.example.launcherapp.domain.usecase.GetAppUsageSnapshotsUseCase
import com.example.launcherapp.domain.usecase.RecordAppUsageDeltaUseCase
import com.example.launcherapp.domain.usecase.ResetDailyUsageIfNeededUseCase
import com.example.launcherapp.domain.usecase.UpdateAllowedPackagesUseCase
import java.util.concurrent.TimeUnit
import java.util.Calendar

/**
 * Periodic safety-net that keeps usage snapshots in sync and prunes exhausted apps.
 *
 * The accessibility service gives us near real-time enforcement whenever the foreground
 * app changes. This worker complements it by:
 *
 * 1. Resetting the stored day marker when the calendar date rolls over. (The reset
 *    happens on the next tick after midnight, not every 15 minutes.)
 * 2. Sampling aggregated UsageStats for every package to capture activity the
 *    accessibility service may have missed (e.g. while disabled, in doze, or when the
 *    device was idle).
 * 3. Removing packages from the allowed set once their recorded quota is fully
 *    consumed.
 */
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

    override suspend fun doWork(): Result {
        Log.d(TAG,"Worker capture started")
        if (!UsageAccessUtils.hasUsageAccess(applicationContext)) {
            return Result.success()
        }

        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        resetDailyUsage(now)

        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val snapshots = getUsageSnapshots()
        val aggregated = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfDay,
            now
        )

        val deltas = mutableMapOf<String, Long>()
        aggregated?.forEach { stats ->
            val pkg = stats.packageName ?: return@forEach
            val total = stats.totalTimeInForeground
            if (total <= 0L) return@forEach
            val previous = snapshots[pkg]?.usedMillisToday ?: 0L
            val delta = total - previous
            if (delta > 0L) {
                deltas[pkg] = delta
            }
        }

        if (deltas.isNotEmpty()) {
            Log.d(TAG, "Recording usage deltas: $deltas")
            recordUsageDelta(deltas)
        }

        val updatedSnapshots = if (deltas.isNotEmpty()) getUsageSnapshots() else snapshots

        val allowedPackages = getAllowedPackages().toMutableSet()
        if (allowedPackages.isNotEmpty()) {
            var changed = false
            allowedPackages.toList().forEach { pkg ->
                val snapshot = updatedSnapshots[pkg]
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
        private const val TAG = "NebulaUsageWorker"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<AppUsageMonitorWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "app_usage_monitor_startup",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<AppUsageMonitorWorker>().build() //TODO: REplave this strategy with some other thing
            )

        }

    }
}
