package com.example.launcherapp.presentation.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.launcherapp.domain.model.AppInfo
import com.example.launcherapp.domain.model.AppUsageSnapshot
import com.example.launcherapp.domain.usecase.GetAllowedPackagesUseCase
import com.example.launcherapp.domain.usecase.GetAppUsageSnapshotsUseCase
import com.example.launcherapp.domain.usecase.GetLaunchableAppsUseCase
import com.example.launcherapp.domain.usecase.IsSelectionLockedUseCase
import com.example.launcherapp.domain.usecase.ResetDailyUsageIfNeededUseCase
import com.example.launcherapp.domain.usecase.SetSelectionLockedUseCase
import com.example.launcherapp.domain.usecase.UpdateAllowedPackagesUseCase
import com.example.launcherapp.domain.usecase.UpdateAppUsageLimitsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class BaseLauncherViewModel(
    private val getLaunchableAppsUseCase: GetLaunchableAppsUseCase,
    private val getAllowedPackagesUseCase: GetAllowedPackagesUseCase,
    private val updateAllowedPackagesUseCase: UpdateAllowedPackagesUseCase,
    private val isSelectionLockedUseCase: IsSelectionLockedUseCase,
    private val setSelectionLockedUseCase: SetSelectionLockedUseCase,
    private val getAppUsageSnapshotsUseCase: GetAppUsageSnapshotsUseCase,
    private val updateAppUsageLimitsUseCase: UpdateAppUsageLimitsUseCase,
    private val resetDailyUsageIfNeededUseCase: ResetDailyUsageIfNeededUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LauncherUiState(isLoading = true))
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadApps(forceRefresh: Boolean = false) {
        if (loadJob?.isActive == true || (!forceRefresh && _uiState.value.availableApps.isNotEmpty())) {
            return
        }

        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val allowedPackages = runCatching { getAllowedPackagesUseCase() }.getOrElse { emptySet() }
            val locked = runCatching { isSelectionLockedUseCase() }.getOrElse { false }
            val usageSnapshots = runCatching { fetchUsageSnapshots() }.getOrElse { emptyMap() }
            runCatching { getLaunchableAppsUseCase() }
                .onSuccess { apps ->
                    _uiState.value = LauncherUiState(
                        isLoading = false,
                        availableApps = apps,
                        visibleApps = filterVisibleApps(apps, allowedPackages, usageSnapshots),
                        allowedPackages = allowedPackages,
                        selectionLocked = locked,
                        usageSnapshots = usageSnapshots,
                        error = null
                    )
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "Unable to load apps"
                    _uiState.value = LauncherUiState(
                        isLoading = false,
                        availableApps = emptyList(),
                        visibleApps = emptyList(),
                        allowedPackages = allowedPackages,
                        selectionLocked = locked,
                        usageSnapshots = usageSnapshots,
                        error = message
                    )
                }
        }
    }

    fun refreshUsage() {
        viewModelScope.launch {
            val snapshots = runCatching { fetchUsageSnapshots() }.getOrElse { emptyMap() }
            val current = _uiState.value
            _uiState.value = current.copy(
                usageSnapshots = snapshots,
                visibleApps = filterVisibleApps(current.availableApps, current.allowedPackages, snapshots)
            )
        }
    }

    fun applySelection(
        packages: Set<String>,
        usageLimitsMinutes: Map<String, Int?>,
        lockSelection: Boolean
    ) {
        viewModelScope.launch {
            runCatching { updateAllowedPackagesUseCase(packages) }
            val normalizedLimits = mutableMapOf<String, Int?>()
            packages.forEach { pkg ->
                normalizedLimits[pkg] = usageLimitsMinutes[pkg]
            }
            _uiState.value.usageSnapshots.keys
                .filter { it !in packages }
                .forEach { pkg -> normalizedLimits[pkg] = null }
            runCatching { updateAppUsageLimitsUseCase(normalizedLimits) }
            if (lockSelection) {
                runCatching { setSelectionLockedUseCase(true) }
            }
            val snapshots = runCatching { fetchUsageSnapshots() }.getOrElse { emptyMap() }
            val currentApps = _uiState.value.availableApps
            _uiState.value = _uiState.value.copy(
                allowedPackages = packages,
                selectionLocked = _uiState.value.selectionLocked || lockSelection,
                usageSnapshots = snapshots,
                visibleApps = filterVisibleApps(currentApps, packages, snapshots)
            )
        }
    }

    fun unlockSelection() {
        viewModelScope.launch {
            runCatching { setSelectionLockedUseCase(false) }
            _uiState.value = _uiState.value.copy(selectionLocked = false)
        }
    }

    private suspend fun fetchUsageSnapshots(): Map<String, AppUsageSnapshot> {
        resetDailyUsageIfNeededUseCase(System.currentTimeMillis())
        return getAppUsageSnapshotsUseCase()
    }

    private fun filterVisibleApps(
        apps: List<AppInfo>,
        allowed: Set<String>,
        usageSnapshots: Map<String, AppUsageSnapshot>
    ): List<AppInfo> {
        val allowedFiltered = if (allowed.isEmpty()) {
            apps
        } else {
            apps.filter { allowed.contains(it.packageName) }
        }
        if (usageSnapshots.isEmpty()) return allowedFiltered
        return allowedFiltered.filter { info ->
            val snapshot = usageSnapshots[info.packageName]
            if (snapshot == null) {
                true
            } else {
                val remaining = snapshot.remainingMinutes
                remaining == null || remaining > 0
            }
        }
    }
}

class ActiveLauncherViewModel(
    getLaunchableAppsUseCase: GetLaunchableAppsUseCase,
    getAllowedPackagesUseCase: GetAllowedPackagesUseCase,
    updateAllowedPackagesUseCase: UpdateAllowedPackagesUseCase,
    isSelectionLockedUseCase: IsSelectionLockedUseCase,
    setSelectionLockedUseCase: SetSelectionLockedUseCase,
    getAppUsageSnapshotsUseCase: GetAppUsageSnapshotsUseCase,
    updateAppUsageLimitsUseCase: UpdateAppUsageLimitsUseCase,
    resetDailyUsageIfNeededUseCase: ResetDailyUsageIfNeededUseCase
) : BaseLauncherViewModel(
    getLaunchableAppsUseCase,
    getAllowedPackagesUseCase,
    updateAllowedPackagesUseCase,
    isSelectionLockedUseCase,
    setSelectionLockedUseCase,
    getAppUsageSnapshotsUseCase,
    updateAppUsageLimitsUseCase,
    resetDailyUsageIfNeededUseCase
)

class PreviewLauncherViewModel(
    getLaunchableAppsUseCase: GetLaunchableAppsUseCase,
    getAllowedPackagesUseCase: GetAllowedPackagesUseCase,
    updateAllowedPackagesUseCase: UpdateAllowedPackagesUseCase,
    isSelectionLockedUseCase: IsSelectionLockedUseCase,
    setSelectionLockedUseCase: SetSelectionLockedUseCase,
    getAppUsageSnapshotsUseCase: GetAppUsageSnapshotsUseCase,
    updateAppUsageLimitsUseCase: UpdateAppUsageLimitsUseCase,
    resetDailyUsageIfNeededUseCase: ResetDailyUsageIfNeededUseCase
) : BaseLauncherViewModel(
    getLaunchableAppsUseCase,
    getAllowedPackagesUseCase,
    updateAllowedPackagesUseCase,
    isSelectionLockedUseCase,
    setSelectionLockedUseCase,
    getAppUsageSnapshotsUseCase,
    updateAppUsageLimitsUseCase,
    resetDailyUsageIfNeededUseCase
)
