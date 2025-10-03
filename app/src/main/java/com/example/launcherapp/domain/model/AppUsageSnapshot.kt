package com.example.launcherapp.domain.model

data class AppUsageSnapshot(
    val packageName: String,
    val limitMinutes: Int?,
    val usedMillisToday: Long
) {
    val remainingMinutes: Int?
        get() = limitMinutes?.let { limit ->
            val consumedMinutes = (usedMillisToday / 60000.0).toInt()
            (limit - consumedMinutes).coerceAtLeast(0)
        }
}
