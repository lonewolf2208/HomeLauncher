package com.example.launcherapp.presentation.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.launcherapp.domain.model.AppInfo
import com.example.launcherapp.domain.model.AppUsageSnapshot
import com.example.launcherapp.drawableToImageBitmap
import com.example.launcherapp.ui.theme.LauncherAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActiveLauncherScreen(
    uiState: LauncherUiState,
    showPasswordPrompt: Boolean,
    passwordError: String?,
    onAttemptUnlock: (String) -> Unit,
    onCancelUnlock: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit
) {
    LauncherScreenScaffold(
        isHomeLauncher = true,
        showPasswordPrompt = showPasswordPrompt,
        passwordError = passwordError,
        onAttemptUnlock = onAttemptUnlock,
        onCancelUnlock = onCancelUnlock
    ) { currentTime, _ ->
        ActiveLauncherContent(
            currentTime = currentTime,
            uiState = uiState,
            onOpenSettings = onOpenSettings,
            onOpenApp = onOpenApp,
            onLongPressApp = onLongPressApp
        )
    }
}

@Composable
private fun ActiveLauncherContent(
    currentTime: Date,
    uiState: LauncherUiState,
    onOpenSettings: () -> Unit,
    onOpenApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val timeText = remember(currentTime) { timeFormatter.format(currentTime) }
    val dateText = remember(currentTime) { dateFormatter.format(currentTime) }
    val apps = uiState.visibleApps
    val usage = uiState.usageSnapshots

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                EmptyState()
            }
        } else {
            NiagaraAppList(
                apps = apps,
                usage = usage,
                modifier = Modifier.weight(1f, fill = true),
                onOpenApp = onOpenApp,
                onLongPressApp = onLongPressApp
            )
        }

        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Open settings",
                modifier = Modifier.padding(vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun NiagaraAppList(
    apps: List<AppInfo>,
    usage: Map<String, AppUsageSnapshot>,
    modifier: Modifier,
    onOpenApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            NiagaraAppRow(
                app = app,
                snapshot = usage[app.packageName],
                onOpenApp = onOpenApp,
                onLongPressApp = onLongPressApp
            )
        }
    }
}

@Composable
private fun NiagaraAppRow(
    app: AppInfo,
    snapshot: AppUsageSnapshot?,
    onOpenApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit
) {
    val icon = rememberAppIcon(app.packageName)
    val remainingLabel = remember(snapshot) {
        when {
            snapshot == null || snapshot.limitMinutes == null -> "Unlimited"
            (snapshot.remainingMinutes ?: 0) <= 0 -> "Limit reached"
            else -> "${snapshot.remainingMinutes ?: 0} min left"
        }
    }
    val labelColor = when {
        snapshot == null || snapshot.limitMinutes == null -> MaterialTheme.colorScheme.onSurfaceVariant
        (snapshot.remainingMinutes ?: 0) <= 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .homeAppInteraction(
                    onClick = { onOpenApp(app) },
                    onLongPress = { onLongPressApp(app) }
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Image(bitmap = icon, contentDescription = app.label, modifier = Modifier.size(36.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = remainingLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "No apps yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Use the settings button below to choose which apps appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun rememberAppIcon(packageName: String): ImageBitmap {
    val pm = LocalContext.current.packageManager
    return remember(packageName) {
        runCatching {
            val drawable = pm.getApplicationIcon(packageName)
            drawableToImageBitmap(drawable)
        }.getOrElse {
            drawableToImageBitmap(pm.defaultActivityIcon)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.homeAppInteraction(
    onClick: () -> Unit,
    onLongPress: () -> Unit
): Modifier = this.then(
    Modifier.combinedClickable(
        onClick = onClick,
        onLongClick = onLongPress
    )
)

private val previewActiveApps = listOf(
    AppInfo("Nebula Chat", "com.nebula.chat"),
    AppInfo("Orbital Studio", "com.nebula.studio"),
    AppInfo("Galaxy Maps", "com.nebula.maps"),
    AppInfo("Star Tunes", "com.nebula.music")
)

private val previewActiveUsage = mapOf(
    "com.nebula.chat" to AppUsageSnapshot(
        packageName = "com.nebula.chat",
        limitMinutes = 90,
        usedMillisToday = 40 * 60 * 1000L
    ),
    "com.nebula.music" to AppUsageSnapshot(
        packageName = "com.nebula.music",
        limitMinutes = 30,
        usedMillisToday = 10 * 60 * 1000L
    ),
    "com.nebula.maps" to AppUsageSnapshot(
        packageName = "com.nebula.maps",
        limitMinutes = null,
        usedMillisToday = 5 * 60 * 1000L
    )
)

private val previewActiveUiState = LauncherUiState(
    availableApps = previewActiveApps,
    visibleApps = previewActiveApps,
    allowedPackages = previewActiveApps.map { it.packageName }.toSet(),
    selectionLocked = false,
    usageSnapshots = previewActiveUsage
)

@Preview(name = "Active Launcher", showBackground = true, backgroundColor = 0xFF050512)
@Composable
private fun ActiveLauncherScreenPreview() {
    LauncherAppTheme(darkTheme = true) {
        ActiveLauncherScreen(
            uiState = previewActiveUiState,
            showPasswordPrompt = false,
            passwordError = null,
            onAttemptUnlock = {},
            onCancelUnlock = {},
            onOpenSettings = {},
            onOpenApp = {},
            onLongPressApp = {}
        )
    }
}
