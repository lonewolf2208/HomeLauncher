package com.example.launcherapp.presentation.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    ) { currentTime, currentTimeText ->
        PreviewLauncherContent(
            currentTime = currentTime,
            currentTimeText = currentTimeText,
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
    currentTimeText: String,
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            LauncherHeader(
                timeText = currentTimeText,
                appCount = uiState.visibleApps.size,
                isHomeLauncher = false
            )
        }

        item { StatusBanner(isHomeLauncher = false) }

        item {
            PreviewSummaryCard(
                friendlyDate = friendlyDate,
                appCount = uiState.visibleApps.size,
                hasUsageAccess = hasUsageAccess,
                isAccessibilityEnabled = isAccessibilityEnabled,
                onRequestSetLauncher = onRequestSetLauncher,
                onRequestUsagePermission = onRequestUsagePermission,
                onRequestAccessibility = onRequestAccessibility
            )
        }

        item {
            PreviewActionRow(
                onRequestSetLauncher = onRequestSetLauncher,
                onOpenDefaultSettings = onOpenDefaultSettings
            )
        }

        item {
            PreviewAppList(
                apps = uiState.visibleApps,
                usageSnapshots = uiState.usageSnapshots,
                onManageApp = onRequestSetLauncher,
                onOpenDefaultSettings = onOpenDefaultSettings
            )
        }
    }
}

@Composable
private fun PreviewSummaryCard(
    friendlyDate: String,
    appCount: Int,
    hasUsageAccess: Boolean,
    isAccessibilityEnabled: Boolean,
    onRequestSetLauncher: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onRequestAccessibility: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Here’s your curated universe",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$appCount app${if (appCount == 1) "" else "s"} ready to launch",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Text(
                text = friendlyDate,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            if (!hasUsageAccess) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Usage access required",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = onRequestUsagePermission) {
                                Text("Grant access", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            if (!isAccessibilityEnabled) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Enable live blocking",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Turn on Nebula's accessibility service so time limits take effect immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        TextButton(onClick = onRequestAccessibility) {
                            Text("Open accessibility settings")
                        }
                    }
                }
            }

            Button(
                onClick = onRequestSetLauncher,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Pick or remove apps", color = MaterialTheme.colorScheme.onSecondary)
            }
        }
    }
}

@Composable
private fun PreviewActionRow(
    onRequestSetLauncher: () -> Unit,
    onOpenDefaultSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = onRequestSetLauncher,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Edit selection")
        }
        TextButton(
            modifier = Modifier.weight(1f),
            onClick = onOpenDefaultSettings
        ) {
            Text("Default launcher settings")
        }
    }
}

@Composable
private fun PreviewAppList(
    apps: List<AppInfo>,
    usageSnapshots: Map<String, AppUsageSnapshot>,
    onManageApp: () -> Unit,
    onOpenDefaultSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Apps in this launcher",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onManageApp) {
                    Text("Manage")
                }
            }

            if (apps.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "No apps selected yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    TextButton(onClick = onManageApp) { Text("Pick apps") }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    apps.forEach { app ->
                        val snapshot = usageSnapshots[app.packageName]
                        PreviewAppRow(
                            app = app,
                            usageSnapshot = snapshot,
                            onManageApp = onManageApp
                        )
                    }
                }
            }

            TextButton(onClick = onOpenDefaultSettings) {
                Text("Open system home settings")
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
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = usageSnapshot.remainingLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        IconButton(onClick = onManageApp) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

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
