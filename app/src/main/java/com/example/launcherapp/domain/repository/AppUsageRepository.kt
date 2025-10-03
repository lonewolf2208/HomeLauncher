package com.example.launcherapp.domain.repository

import com.example.launcherapp.domain.model.AppUsageSnapshot

interface AppUsageRepository {
    suspend fun getUsageSnapshots(): Map<String, AppUsageSnapshot>
    suspend fun updateUsageLimits(limitsMinutes: Map<String, Int?>)
    suspend fun recordUsageDelta(deltaMillisByPackage: Map<String, Long>)
    suspend fun resetDailyUsageIfNeeded(currentTimeMillis: Long)
    suspend fun setLastUsageCheck(timestampMillis: Long)
    suspend fun getLastUsageCheck(): Long
}
