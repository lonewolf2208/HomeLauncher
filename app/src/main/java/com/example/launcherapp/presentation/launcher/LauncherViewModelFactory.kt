package com.example.launcherapp.presentation.launcher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.launcherapp.data.repository.AccessControlRepositoryImpl
import com.example.launcherapp.data.repository.LauncherRepositoryImpl
import com.example.launcherapp.data.usage.AppUsageRepositoryImpl
import com.example.launcherapp.domain.usecase.GetAllowedPackagesUseCase
import com.example.launcherapp.domain.usecase.GetAppUsageSnapshotsUseCase
import com.example.launcherapp.domain.usecase.GetLaunchableAppsUseCase
import com.example.launcherapp.domain.usecase.IsSelectionLockedUseCase
import com.example.launcherapp.domain.usecase.ResetDailyUsageIfNeededUseCase
import com.example.launcherapp.domain.usecase.SetSelectionLockedUseCase
import com.example.launcherapp.domain.usecase.UpdateAllowedPackagesUseCase
import com.example.launcherapp.domain.usecase.UpdateAppUsageLimitsUseCase

class LauncherViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val applicationContext = context.applicationContext

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val launcherRepository = LauncherRepositoryImpl(applicationContext)
        val accessControlRepository = AccessControlRepositoryImpl(applicationContext)
        val usageRepository = AppUsageRepositoryImpl(applicationContext)
        val getApps = GetLaunchableAppsUseCase(launcherRepository)
        val getAllowed = GetAllowedPackagesUseCase(accessControlRepository)
        val updateAllowed = UpdateAllowedPackagesUseCase(accessControlRepository)
        val isLocked = IsSelectionLockedUseCase(accessControlRepository)
        val setLocked = SetSelectionLockedUseCase(accessControlRepository)
        val getUsage = GetAppUsageSnapshotsUseCase(usageRepository)
        val setLimits = UpdateAppUsageLimitsUseCase(usageRepository)
        val resetDaily = ResetDailyUsageIfNeededUseCase(usageRepository)

        val dependencies = LauncherDependencies(
            getApps = getApps,
            getAllowed = getAllowed,
            updateAllowed = updateAllowed,
            isLocked = isLocked,
            setLocked = setLocked,
            getUsage = getUsage,
            setLimits = setLimits,
            resetDaily = resetDaily
        )

        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(ActiveLauncherViewModel::class.java) -> {
                dependencies.toActiveViewModel()
            }

            modelClass.isAssignableFrom(PreviewLauncherViewModel::class.java) -> {
                dependencies.toPreviewViewModel()
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class ${'$'}{modelClass.name}")
        } as T
    }
}

private data class LauncherDependencies(
    val getApps: GetLaunchableAppsUseCase,
    val getAllowed: GetAllowedPackagesUseCase,
    val updateAllowed: UpdateAllowedPackagesUseCase,
    val isLocked: IsSelectionLockedUseCase,
    val setLocked: SetSelectionLockedUseCase,
    val getUsage: GetAppUsageSnapshotsUseCase,
    val setLimits: UpdateAppUsageLimitsUseCase,
    val resetDaily: ResetDailyUsageIfNeededUseCase
)

private fun LauncherDependencies.toActiveViewModel(): ActiveLauncherViewModel {
    return ActiveLauncherViewModel(
        getLaunchableAppsUseCase = getApps,
        getAllowedPackagesUseCase = getAllowed,
        updateAllowedPackagesUseCase = updateAllowed,
        isSelectionLockedUseCase = isLocked,
        setSelectionLockedUseCase = setLocked,
        getAppUsageSnapshotsUseCase = getUsage,
        updateAppUsageLimitsUseCase = setLimits,
        resetDailyUsageIfNeededUseCase = resetDaily
    )
}

private fun LauncherDependencies.toPreviewViewModel(): PreviewLauncherViewModel {
    return PreviewLauncherViewModel(
        getLaunchableAppsUseCase = getApps,
        getAllowedPackagesUseCase = getAllowed,
        updateAllowedPackagesUseCase = updateAllowed,
        isSelectionLockedUseCase = isLocked,
        setSelectionLockedUseCase = setLocked,
        getAppUsageSnapshotsUseCase = getUsage,
        updateAppUsageLimitsUseCase = setLimits,
        resetDailyUsageIfNeededUseCase = resetDaily
    )
}
