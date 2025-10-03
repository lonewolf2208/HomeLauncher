package com.example.launcherapp.presentation.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.launcherapp.domain.model.AppInfo

@Composable
fun LauncherActivityContent(
    isHomeLauncher: Boolean,
    hasUsageAccess: Boolean,
    showPasswordPrompt: Boolean,
    passwordError: String?,
    activeViewModel: ActiveLauncherViewModel,
    previewViewModel: PreviewLauncherViewModel,
    onAttemptUnlock: (String) -> Unit,
    onCancelUnlock: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit,
    onRequestUsagePermission: () -> Unit,
    onSelectionApplied: () -> Unit
) {
    val activeUiState by activeViewModel.uiState.collectAsState()
    val previewUiState by previewViewModel.uiState.collectAsState()

    val currentViewModel = if (isHomeLauncher) activeViewModel else previewViewModel
    val currentUiState = if (isHomeLauncher) activeUiState else previewUiState

    var showAppSelection by remember { mutableStateOf(false) }
    var selectedPackages by remember { mutableStateOf(currentUiState.allowedPackages.toSet()) }

    LaunchedEffect(isHomeLauncher) {
        if (isHomeLauncher) {
            activeViewModel.loadApps()
            activeViewModel.refreshUsage()
        } else {
            previewViewModel.loadApps()
            previewViewModel.refreshUsage()
        }
        showAppSelection = false
    }

    LaunchedEffect(currentUiState.allowedPackages) {
        if (!showAppSelection) {
            selectedPackages = currentUiState.allowedPackages.toSet()
        }
    }

    val onRequestSetLauncher: () -> Unit = {
        if (!currentUiState.selectionLocked && currentUiState.availableApps.isNotEmpty()) {
            selectedPackages = if (currentUiState.allowedPackages.isNotEmpty()) {
                currentUiState.allowedPackages.toSet()
            } else {
                emptySet()
            }
            showAppSelection = true
        }
    }

    if (isHomeLauncher) {
        ActiveLauncherScreen(
            uiState = activeUiState,
            showPasswordPrompt = showPasswordPrompt,
            passwordError = passwordError,
            onAttemptUnlock = onAttemptUnlock,
            onCancelUnlock = onCancelUnlock,
            onRequestSetLauncher = onRequestSetLauncher,
            onOpenDefaultSettings = onOpenDefaultSettings,
            onOpenSystemSettings = onOpenSystemSettings,
            onOpenApp = onOpenApp,
            onLongPressApp = onLongPressApp
        )
    } else {
        PreviewLauncherScreen(
            uiState = previewUiState,
            showPasswordPrompt = showPasswordPrompt,
            passwordError = passwordError,
            hasUsageAccess = hasUsageAccess,
            onAttemptUnlock = onAttemptUnlock,
            onCancelUnlock = onCancelUnlock,
            onRequestSetLauncher = onRequestSetLauncher,
            onOpenDefaultSettings = onOpenDefaultSettings,
            onRequestUsagePermission = onRequestUsagePermission
        )
    }

    if (showAppSelection) {
        AppSelectionDialog(
            apps = currentUiState.availableApps,
            initialSelection = selectedPackages,
            initialLimits = currentUiState.usageSnapshots.mapValues { it.value.limitMinutes },
            onConfirm = { selection, limits ->
                if (selection.isNotEmpty()) {
                    val confirmed = selection.toSet()
                    selectedPackages = confirmed
                    currentViewModel.applySelection(
                        packages = confirmed,
                        usageLimitsMinutes = limits,
                        lockSelection = true
                    )
                    showAppSelection = false
                    onSelectionApplied()
                }
            },
            onDismiss = { showAppSelection = false }
        )
    }
}
