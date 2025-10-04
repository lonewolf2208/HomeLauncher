package com.example.launcherapp.presentation.launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.launcherapp.domain.model.AppInfo
import com.example.launcherapp.domain.model.AppUsageSnapshot

@Composable
fun LauncherScreen(
    uiState: LauncherUiState,
    showPasswordPrompt: Boolean,
    passwordError: String?,
    isHomeLauncher: Boolean,
    hasUsageAccess: Boolean,
    onAttemptUnlock: (String) -> Unit,
    onCancelUnlock: () -> Unit,
    onRequestSetLauncher: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit,
    onRequestUsagePermission: () -> Unit
) {
    if (isHomeLauncher) {
        ActiveLauncherScreen(
            uiState = uiState,
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
//        PreviewLauncherScreen(
//            uiState = uiState,
//            showPasswordPrompt = showPasswordPrompt,
//            passwordError = passwordError,
//            hasUsageAccess = hasUsageAccess,
//            onAttemptUnlock = onAttemptUnlock,
//            onCancelUnlock = onCancelUnlock,
//            onRequestSetLauncher = onRequestSetLauncher,
//            onOpenDefaultSettings = onOpenDefaultSettings,
//            onRequestUsagePermission = onRequestUsagePermission,
//        )
    }
}

private val previewAppChat = AppInfo(
    label = "Nebula Chat",
    packageName = "com.nebula.chat"
)
private val previewAppStudio = AppInfo(
    label = "Orbital Studio",
    packageName = "com.nebula.studio"
)
private val previewAppNavigator = AppInfo(
    label = "Galaxy Maps",
    packageName = "com.nebula.maps"
)

private val previewApps = listOf(previewAppChat, previewAppStudio, previewAppNavigator)

private val previewUsageSnapshots = mapOf(
    previewAppChat.packageName to AppUsageSnapshot(
        packageName = previewAppChat.packageName,
        limitMinutes = 90,
        usedMillisToday = 25 * 60 * 1000L
    ),
    previewAppStudio.packageName to AppUsageSnapshot(
        packageName = previewAppStudio.packageName,
        limitMinutes = 45,
        usedMillisToday = 44 * 60 * 1000L
    )
)

private val previewActiveUiState = LauncherUiState(
    availableApps = previewApps,
    visibleApps = previewApps,
    allowedPackages = previewApps.map { it.packageName }.toSet(),
    selectionLocked = false,
    usageSnapshots = previewUsageSnapshots
)

private val previewSetupUiState = LauncherUiState(
    availableApps = previewApps,
    visibleApps = previewApps.take(2),
    allowedPackages = previewApps.take(2).map { it.packageName }.toSet(),
    selectionLocked = true,
    usageSnapshots = previewUsageSnapshots
)

@Preview(name = "Launcher · Active", showBackground = true, backgroundColor = 0xFF050512)
@Composable
private fun LauncherScreenActivePreview() {
    ActiveLauncherScreen(
        uiState = previewActiveUiState,
        showPasswordPrompt = false,
        passwordError = null,
        onAttemptUnlock = {},
        onCancelUnlock = {},
        onRequestSetLauncher = {},
        onOpenDefaultSettings = {},
        onOpenSystemSettings = {},
        onOpenApp = {},
        onLongPressApp = {}
    )
}

@Preview(name = "Launcher · Setup", showBackground = true, backgroundColor = 0xFF120718)
@Composable
private fun LauncherScreenSetupPreview() {
//    PreviewLauncherScreen(
//        uiState = previewSetupUiState,
//        showPasswordPrompt = false,
//        passwordError = null,
//        hasUsageAccess = false,
//        onAttemptUnlock = {},
//        onCancelUnlock = {},
//        onRequestSetLauncher = {},
//        onOpenDefaultSettings = {},
//        onRequestUsagePermission = {}
//    )
}

@Preview(name = "Launcher · Unlock Prompt", showBackground = true, backgroundColor = 0xFF050512)
@Composable
private fun LauncherScreenUnlockPreview() {
    ActiveLauncherScreen(
        uiState = previewActiveUiState,
        showPasswordPrompt = true,
        passwordError = "Incorrect password. Try again.",
        onAttemptUnlock = {},
        onCancelUnlock = {},
        onRequestSetLauncher = {},
        onOpenDefaultSettings = {},
        onOpenSystemSettings = {},
        onOpenApp = {},
        onLongPressApp = {}
    )
}
