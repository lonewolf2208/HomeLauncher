package com.example.launcherapp.presentation.launcher

import com.example.launcherapp.domain.model.AppInfo
import com.example.launcherapp.domain.model.AppUsageSnapshot

data class LauncherUiState(
    val isLoading: Boolean = false,
    val availableApps: List<AppInfo> = emptyList(),
    val visibleApps: List<AppInfo> = emptyList(),
    val allowedPackages: Set<String> = emptySet(),
    val selectionLocked: Boolean = false,
    val usageSnapshots: Map<String, AppUsageSnapshot> = emptyMap(),
    val error: String? = null
)
