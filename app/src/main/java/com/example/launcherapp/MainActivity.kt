package com.example.launcherapp

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.launcherapp.domain.model.AppInfo
import com.example.launcherapp.data.usage.AppUsageMonitorWorker
import com.example.launcherapp.data.usage.UsageAccessUtils
import com.example.launcherapp.presentation.launcher.ActiveLauncherViewModel
import com.example.launcherapp.presentation.launcher.LauncherActivityContent
import com.example.launcherapp.presentation.launcher.LauncherViewModelFactory
import com.example.launcherapp.presentation.launcher.PreviewLauncherViewModel

class MainActivity : ComponentActivity() {

    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* result handled by system */ }

    private val activeLauncherViewModel: ActiveLauncherViewModel by viewModels {
        LauncherViewModelFactory(this)
    }

    private val previewLauncherViewModel: PreviewLauncherViewModel by viewModels {
        LauncherViewModelFactory(this)
    }

    private var isUnlocked by mutableStateOf(false)
    private val showPasswordPrompt = mutableStateOf(false)
    private val passwordError = mutableStateOf<String?>(null)
    private var pendingUnlockAction: (() -> Unit)? = null
    private val isHomeLauncherState = mutableStateOf(false)
    private var suppressNextLeaveGuard = false
    private val usageAccessGrantedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isHomeLauncherState.value = isDefaultLauncher()
        usageAccessGrantedState.value = UsageAccessUtils.hasUsageAccess(this)
        AppUsageMonitorWorker.enqueue(this)
        setContent {
            LauncherActivityContent(
                isHomeLauncher = isHomeLauncherState.value,
                hasUsageAccess = usageAccessGrantedState.value,
                showPasswordPrompt = showPasswordPrompt.value,
                passwordError = passwordError.value,
                activeViewModel = activeLauncherViewModel,
                previewViewModel = previewLauncherViewModel,
                onAttemptUnlock = { attemptUnlock(it) },
                onCancelUnlock = { cancelUnlock() },
                onOpenDefaultSettings = { ensureUnlocked { openDefaultAppsSettings() } },
                onOpenSystemSettings = { ensureUnlocked { openSystemSettings() } },
                onOpenApp = { app -> openApp(app) },
                onLongPressApp = { app -> ensureUnlocked { openAppDetails(app) } },
                onRequestUsagePermission = { UsageAccessUtils.openUsageAccessSettings(this) },
                onSelectionApplied = { ensureUnlocked { requestHomeRole() } }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        isUnlocked = false
        if (!isDefaultLauncher()) {
            if (previewLauncherViewModel.uiState.value.selectionLocked) {
                previewLauncherViewModel.unlockSelection()
            }
        }
        isHomeLauncherState.value = isDefaultLauncher()
        suppressNextLeaveGuard = false
        usageAccessGrantedState.value = UsageAccessUtils.hasUsageAccess(this)
        activeLauncherViewModel.loadApps(forceRefresh = true)
        activeLauncherViewModel.refreshUsage()
        previewLauncherViewModel.loadApps(forceRefresh = true)
        previewLauncherViewModel.refreshUsage()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isHomeLauncherState.value) {
            isUnlocked = false
            return
        }
        if (suppressNextLeaveGuard) {
            suppressNextLeaveGuard = false
            isUnlocked = false
            return
        }
        if (!isUnlocked) {
            if (!showPasswordPrompt.value) {
                requirePassword()
            }
            Handler(Looper.getMainLooper()).post {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
        }
    }

    private fun ensureUnlocked(action: () -> Unit) {
        if (!isHomeLauncherState.value) {
            action()
            return
        }
        if (isUnlocked) {
            action()
        } else {
            requirePassword(action)
        }
    }

    private fun requirePassword(action: (() -> Unit)? = null) {
        pendingUnlockAction = action
        passwordError.value = null
        showPasswordPrompt.value = true
    }

    private fun attemptUnlock(input: String) {
        val expectedPassword = "1234"
        if (input == expectedPassword) {
            isUnlocked = true
            showPasswordPrompt.value = false
            passwordError.value = null
            pendingUnlockAction?.invoke()
            pendingUnlockAction = null
        } else {
            passwordError.value = "Incorrect password"
        }
    }

    private fun cancelUnlock() {
        showPasswordPrompt.value = false
        passwordError.value = null
        pendingUnlockAction = null
    }

    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            roleManager?.let { rm ->
                if (rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    val intent = rm.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    requestRoleLauncher.launch(intent)
                }
            }
        } else {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
        }
    }

    private fun openDefaultAppsSettings() {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        startActivity(intent)
    }

    private fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun openApp(appInfo: AppInfo) {
        val pm = packageManager
        val intent = pm.getLaunchIntentForPackage(appInfo.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent != null) {
            suppressNextLeaveGuard = true
            startActivity(intent)
        }
    }

    private fun openAppDetails(appInfo: AppInfo) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${appInfo.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun isDefaultLauncher(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
        } else {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName == packageName
        }
    }
}
