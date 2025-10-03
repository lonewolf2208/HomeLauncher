package com.example.launcherapp

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.example.launcherapp.data.usage.AppUsageMonitorWorker
import com.example.launcherapp.data.usage.UsageAccessUtils
import com.example.launcherapp.presentation.launcher.AppSelectionDialog
import com.example.launcherapp.presentation.launcher.LauncherViewModelFactory
import com.example.launcherapp.presentation.launcher.PreviewLauncherScreen
import com.example.launcherapp.presentation.launcher.PreviewLauncherViewModel

class PreviewLauncherActivity : ComponentActivity() {

    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* handled by system */ }

    private val launcherViewModel: PreviewLauncherViewModel by viewModels {
        LauncherViewModelFactory(this)
    }

    private val usageAccessGrantedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usageAccessGrantedState.value = UsageAccessUtils.hasUsageAccess(this)

        setContent {
            val uiState by launcherViewModel.uiState.collectAsState()
            val hasUsageAccess by usageAccessGrantedState
            var showAppSelection by remember { mutableStateOf(false) }
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

            PreviewLauncherScreen(
                uiState = uiState,
                showPasswordPrompt = false,
                passwordError = null,
                hasUsageAccess = hasUsageAccess,
                onAttemptUnlock = {},
                onCancelUnlock = {},
                onRequestSetLauncher = {
                    if (uiState.availableApps.isNotEmpty()) {
                        selectedPackages = uiState.allowedPackages.toSet()
                        showAppSelection = true
                    }
                },
                onOpenDefaultSettings = { openDefaultAppsSettings() },
                onRequestUsagePermission = { UsageAccessUtils.openUsageAccessSettings(this) }
            )

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
                                lockSelection = false
                            )
                            showAppSelection = false
                            requestHomeRole()
                        }
                    },
                    onDismiss = { showAppSelection = false }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        usageAccessGrantedState.value = UsageAccessUtils.hasUsageAccess(this)
        if (isDefaultLauncher()) {
            startActivity(
                Intent(this, ActiveLauncherActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
            finish()
            return
        }
        launcherViewModel.loadApps(forceRefresh = true)
        launcherViewModel.refreshUsage()
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
}
