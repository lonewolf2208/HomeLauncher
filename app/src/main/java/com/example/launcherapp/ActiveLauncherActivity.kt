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
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.launcherapp.data.usage.AppUsageMonitorWorker
import com.example.launcherapp.domain.model.AppInfo
import com.example.launcherapp.presentation.launcher.ActiveLauncherScreen
import com.example.launcherapp.presentation.launcher.ActiveLauncherViewModel
import com.example.launcherapp.presentation.launcher.AppSelectionDialog
import com.example.launcherapp.presentation.launcher.LauncherSettingsDialog
import com.example.launcherapp.presentation.launcher.LauncherViewModelFactory
import com.example.launcherapp.ui.theme.LauncherAppTheme

class ActiveLauncherActivity : ComponentActivity() {

    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* result handled by system */ }

    private val launcherViewModel: ActiveLauncherViewModel by viewModels {
        LauncherViewModelFactory(this)
    }

    private var isUnlocked by mutableStateOf(false)
    private val showPasswordPrompt = mutableStateOf(false)
    private val passwordError = mutableStateOf<String?>(null)
    private var pendingUnlockAction: (() -> Unit)? = null
    private var suppressNextLeaveGuard = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUsageMonitorWorker.enqueue(this)
        hideSystemBars()

        setContent {
            LauncherAppTheme(darkTheme = true) {
                val uiState by launcherViewModel.uiState.collectAsState()
                var showAppSelection by remember { mutableStateOf(false) }
                var showSettingsMenu by remember { mutableStateOf(false) }
                var selectedPackages by remember { mutableStateOf(uiState.allowedPackages.toSet()) }

                LaunchedEffect(Unit) {
                    launcherViewModel.loadApps()
                    launcherViewModel.refreshUsage()
                }

                LaunchedEffect(uiState.allowedPackages) {
                    if (!showAppSelection) {
                        selectedPackages = uiState.allowedPackages.toSet()
                    }
                }

                ActiveLauncherScreen(
                    uiState = uiState,
                    showPasswordPrompt = showPasswordPrompt.value,
                    passwordError = passwordError.value,
                    onAttemptUnlock = { attemptUnlock(it) },
                    onCancelUnlock = { cancelUnlock() },
                    onOpenSettings = {
                        ensureUnlocked {
                            showSettingsMenu = true
                        }
                    },
                    onOpenApp = { app -> openApp(app) },
                    onLongPressApp = { app -> ensureUnlocked { openAppDetails(app) } }
                )

                if (showSettingsMenu) {
                    LauncherSettingsDialog(
                        onManageApps = {
                            showSettingsMenu = false
                            if (uiState.availableApps.isNotEmpty()) {
                                selectedPackages = uiState.allowedPackages.toSet()
                                showAppSelection = true
                            }
                        },
                        onChangeLauncher = {
                            showSettingsMenu = false
                            ensureUnlocked {
//                                Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show()
                                openLauncherPicker()
                            }
                        },
                        onDismiss = { showSettingsMenu = false }
                    )
                }

                if (showAppSelection) {
                    AppSelectionDialog(
                        apps = uiState.availableApps,
                        initialSelection = selectedPackages,
                        initialLimits = uiState.usageSnapshots.mapValues { it.value.limitMinutes },
                        onConfirm = { selection, limits ->
                            if (selection.isNotEmpty()) {
                                val confirmed = selection.toSet()
                                selectedPackages = confirmed
                                launcherViewModel.applySelection(
                                    packages = confirmed,
                                    usageLimitsMinutes = limits,
                                    lockSelection = true
                                )
                                showAppSelection = false
                                ensureUnlocked { requestHomeRole() }
                            }
                        },
                        onDismiss = { showAppSelection = false }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        if (!isDefaultLauncher()) {
            startActivity(
                Intent(this, PreviewLauncherActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
            finish()
            return
        }
        isUnlocked = false
        suppressNextLeaveGuard = false
        launcherViewModel.loadApps(forceRefresh = true)
        launcherViewModel.refreshUsage()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isDefaultLauncher()) {
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
                val intent = Intent(this, ActiveLauncherActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
        }
    }

    private fun ensureUnlocked(action: () -> Unit) {
        if (!isDefaultLauncher()) {
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            openDefaultAppsSettings()
            return
        }

        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
            runCatching {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                requestRoleLauncher.launch(intent)
            }.onFailure {
                openDefaultAppsSettings()
            }
        } else {
            openDefaultAppsSettings()
        }
    }

    private fun openLauncherPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            when {
                roleManager == null -> openDefaultAppsSettings()
                roleManager.isRoleHeld(RoleManager.ROLE_HOME) -> openDefaultAppsSettings()
                else -> requestHomeRole()
            }
        } else {
            openDefaultAppsSettings()
        }
    }

    private fun openDefaultAppsSettings() {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
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
