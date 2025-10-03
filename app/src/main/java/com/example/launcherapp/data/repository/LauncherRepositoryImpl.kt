package com.example.launcherapp.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.example.launcherapp.domain.model.AppInfo
import com.example.launcherapp.domain.repository.LauncherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class LauncherRepositoryImpl(private val context: Context) : LauncherRepository {

    override suspend fun getLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm: PackageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val activities: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY)
        activities
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                val label = info.loadLabel(pm)?.toString() ?: info.activityInfo.packageName
                AppInfo(
                    label = label,
                    packageName = info.activityInfo.packageName
                )
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }
}
