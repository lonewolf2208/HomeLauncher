package com.example.launcherapp.domain.usecase

import com.example.launcherapp.domain.model.AppInfo
import com.example.launcherapp.domain.repository.LauncherRepository

class GetLaunchableAppsUseCase(private val repository: LauncherRepository) {
    suspend operator fun invoke(): List<AppInfo> = repository.getLaunchableApps()
}
