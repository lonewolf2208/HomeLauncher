package com.example.launcherapp.presentation.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.launcherapp.domain.model.AppInfo
import com.example.launcherapp.domain.model.AppUsageSnapshot
import androidx.compose.ui.tooling.preview.Preview
import com.example.launcherapp.ui.theme.LauncherAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PreviewLauncherScreen(
    uiState: LauncherUiState,
    showPasswordPrompt: Boolean,
    passwordError: String?,
    hasUsageAccess: Boolean,
    isAccessibilityEnabled: Boolean,
    onAttemptUnlock: (String) -> Unit,
    onCancelUnlock: () -> Unit,
    onRequestSetLauncher: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onRequestAccessibility: () -> Unit
) {
    LauncherScreenScaffold(
        isHomeLauncher = false,
        showPasswordPrompt = showPasswordPrompt,
        passwordError = passwordError,
        onAttemptUnlock = onAttemptUnlock,
        onCancelUnlock = onCancelUnlock
    ) { currentTime, _ ->
        PreviewLauncherContent(
            currentTime = currentTime,
            uiState = uiState,
            hasUsageAccess = hasUsageAccess,
            isAccessibilityEnabled = isAccessibilityEnabled,
            onRequestSetLauncher = onRequestSetLauncher,
            onOpenDefaultSettings = onOpenDefaultSettings,
            onRequestUsagePermission = onRequestUsagePermission,
            onRequestAccessibility = onRequestAccessibility
        )
    }
}

@Composable
private fun PreviewLauncherContent(
    currentTime: Date,
    uiState: LauncherUiState,
    hasUsageAccess: Boolean,
    isAccessibilityEnabled: Boolean,
    onRequestSetLauncher: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onRequestAccessibility: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val friendlyDate = remember(currentTime) { dateFormatter.format(currentTime) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item {
                    LauncherHeader(
                        appCount = uiState.visibleApps.size,
                        isHomeLauncher = false,
                        subtitle = friendlyDate
                    )
                }

                item { StatusBanner(isHomeLauncher = false) }

                item {
                    PreviewOverviewCard(
                        friendlyDate = friendlyDate,
                        appInfo = uiState.visibleApps,
                        usageSnapshots = uiState.usageSnapshots,
                        hasUsageAccess = hasUsageAccess,
                        allowedPackages = uiState.allowedPackages,
                        onRequestUsagePermission = onRequestUsagePermission,
                        onManageApp = onRequestSetLauncher
                    )
                }
            }

            ExtendedFloatingActionButton(
                onClick = onRequestSetLauncher,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(imageVector = Icons.Rounded.Edit, contentDescription = null) },
                text = { Text("Pick or remove apps") }
            )
        }
    }
}

@Composable
private fun PreviewOverviewCard(
    friendlyDate: String,
    appInfo: List<AppInfo>,
    usageSnapshots: Map<String, AppUsageSnapshot>,
    hasUsageAccess: Boolean,
    allowedPackages: Set<String>,
    onRequestUsagePermission: () -> Unit,
    onManageApp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        val curatedApps = remember(appInfo, allowedPackages) {
            if (allowedPackages.isEmpty()) emptyList<AppInfo>() else appInfo.filter { allowedPackages.contains(it.packageName) }
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Here’s your curated universe",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${curatedApps.size} app${if (curatedApps.size == 1) "" else "s"} ready to launch",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!hasUsageAccess) {
                UsagePermissionCard(onRequestUsagePermission)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Apps in the launcher",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (curatedApps.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "No apps selected yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onManageApp) { Text("Pick apps") }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        curatedApps.forEachIndexed { index, app ->
                            val snapshot = usageSnapshots[app.packageName]
                            PreviewAppRow(
                                app = app,
                                usageSnapshot = snapshot,
                                onManageApp = onManageApp
                            )
                            if (index != curatedApps.lastIndex) {
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }

//                TextButton(onClick = onOpenDefaultSettings) {
//                    Text("Open system home settings")
//                }
            }
        }
    }
}

@Composable
private fun PreviewAppRow(
    app: AppInfo,
    usageSnapshot: AppUsageSnapshot?,
    onManageApp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = usageSnapshot.remainingLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UsagePermissionCard(onRequestUsagePermission: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Grant usage access",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "If you want to use Nebula Home's screen time restrictions, grant usage access so we can track time spent in each app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(onClick = onRequestUsagePermission, modifier = Modifier.fillMaxWidth()) {
                Text("Grant permission")
            }
        }
    }
}

//@Composable
//private fun AccessibilityReminder(onRequestAccessibility: () -> Unit) {
//    Column(
//        modifier = Modifier.fillMaxWidth(),
//        verticalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        Text(
//            text = "Enable live blocking",
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.onSurface
//        )
//        Text(
//            text = "Turn on the accessibility service so Nebula Home can close apps the moment a screen time limit is hit.",
//            style = MaterialTheme.typography.bodySmall,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//        TextButton(onClick = onRequestAccessibility) {
//            Text("Open accessibility settings")
//        }
//    }
//}


private fun AppUsageSnapshot?.remainingLabel(): String {
    if (this == null) return "Unlimited"
    val remaining = remainingMinutes
    return if (limitMinutes != null && remaining != null) {
        "$remaining min remaining"
    } else {
        "Unlimited"
    }
}

private val previewSetupApps = listOf(
    AppInfo("Nebula Chat", "com.nebula.chat"),
    AppInfo("Orbital Studio", "com.nebula.studio"),
    AppInfo("Galaxy Maps", "com.nebula.maps")
)

private val previewSetupUsage = mapOf(
    "com.nebula.chat" to AppUsageSnapshot(
        packageName = "com.nebula.chat",
        limitMinutes = 90,
        usedMillisToday = 25 * 60 * 1000L
    )
)

private val previewSetupUiState = LauncherUiState(
    availableApps = previewSetupApps,
    visibleApps = previewSetupApps.take(2),
    allowedPackages = previewSetupApps.take(2).map { it.packageName }.toSet(),
    selectionLocked = true,
    usageSnapshots = previewSetupUsage
)

@Preview(name = "Preview Launcher", showBackground = true, backgroundColor = 0xFF120718)
@Composable
private fun PreviewLauncherScreenPreview() {
    LauncherAppTheme(darkTheme = true) {
        PreviewLauncherScreen(
            uiState = previewSetupUiState,
            showPasswordPrompt = false,
            passwordError = null,
            hasUsageAccess = false,
            isAccessibilityEnabled = false,
            onAttemptUnlock = {},
            onCancelUnlock = {},
            onRequestSetLauncher = {},
            onOpenDefaultSettings = {},
            onRequestUsagePermission = {},
            onRequestAccessibility = {}
        )
    }
}
