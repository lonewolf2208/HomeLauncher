package com.example.launcherapp.domain.repository

import com.example.launcherapp.domain.model.AppInfo

interface LauncherRepository {
    suspend fun getLaunchableApps(): List<AppInfo>
}
