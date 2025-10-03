package com.example.launcherapp.presentation.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.launcherapp.domain.model.AppInfo
import com.example.launcherapp.drawableToImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ActiveLauncherScreen(
    uiState: LauncherUiState,
    showPasswordPrompt: Boolean,
    passwordError: String?,
    onAttemptUnlock: (String) -> Unit,
    onCancelUnlock: () -> Unit,
    onRequestSetLauncher: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit
) {
    LauncherScreenScaffold(
        isHomeLauncher = true,
        showPasswordPrompt = showPasswordPrompt,
        passwordError = passwordError,
        onAttemptUnlock = onAttemptUnlock,
        onCancelUnlock = onCancelUnlock
    ) { currentTime, currentTimeText ->
        ActiveLauncherContent(
            currentTime = currentTime,
            currentTimeText = currentTimeText,
            uiState = uiState,
            onRequestSetLauncher = onRequestSetLauncher,
            onOpenDefaultSettings = onOpenDefaultSettings,
            onOpenSystemSettings = onOpenSystemSettings,
            onOpenApp = onOpenApp,
            onLongPressApp = onLongPressApp
        )
    }
}

@Composable
private fun ActiveLauncherContent(
    currentTime: Date,
    currentTimeText: String,
    uiState: LauncherUiState,
    onRequestSetLauncher: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit
) {
    val calendar = remember(currentTime) { Calendar.getInstance().apply { time = currentTime } }
    val dateFormatter = remember { SimpleDateFormat("EEEE · MMM d", Locale.getDefault()) }
    val friendlyDate = remember(currentTime) { dateFormatter.format(currentTime) }
    val greeting = remember(currentTime) {
        when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Welcome back"
        }
    }
    val timeDisplay = currentTimeText.substringAfter('·', currentTimeText).trim().ifEmpty { currentTimeText }

    val favourites = uiState.visibleApps.take(4)
    val remaining = uiState.visibleApps.drop(4)
    val dockApps = favourites.ifEmpty { uiState.visibleApps.take(4) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = if (dockApps.isNotEmpty()) 140.dp else 32.dp)
        ) {
            item {
                HomeHeroSection(
                    greeting = greeting,
                    timeDisplay = timeDisplay,
                    dateDisplay = friendlyDate,
                    selectionLocked = uiState.selectionLocked,
                    onOpenDefaultSettings = onOpenDefaultSettings,
                    onOpenSystemSettings = onOpenSystemSettings
                )
            }

            when {
                uiState.isLoading -> {
                    item { LoadingState() }
                }

                uiState.error != null -> {
                    item { ErrorState(message = uiState.error) }
                }

                uiState.visibleApps.isEmpty() -> {
                    item { EmptyState(onRequestSetLauncher = onRequestSetLauncher) }
                }

                else -> {
                    if (favourites.isNotEmpty()) {
                        item {
                            FavoritesShelf(
                                apps = favourites,
                                onOpenApp = onOpenApp,
                                onLongPressApp = onLongPressApp
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Apps in Nebula Home",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                        )
                    }

                    val gridSource = if (remaining.isEmpty()) uiState.visibleApps else remaining
                    val rows = gridSource.chunked(3)
                    itemsIndexed(rows) { _, rowApps ->
                        HomeGridRow(
                            apps = rowApps,
                            onOpen = onOpenApp,
                            onLongPress = onLongPressApp
                        )
                    }
                }
            }
        }

        if (dockApps.isNotEmpty()) {
            HomeDock(
                apps = dockApps,
                onOpen = onOpenApp,
                onLongPress = onLongPressApp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
            )
        }
    }
}

@Composable
private fun HomeHeroSection(
    greeting: String,
    timeDisplay: String,
    dateDisplay: String,
    selectionLocked: Boolean,
    onOpenDefaultSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Text(
            text = timeDisplay,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = dateDisplay,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusPill(label = "Nebula Home", tint = MaterialTheme.colorScheme.primary)
            StatusPill(
                label = if (selectionLocked) "Selection locked" else "Selection open",
                tint = if (selectionLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenDefaultSettings) { Text("Manage apps") }
            TextButton(onClick = onOpenSystemSettings) { Text("System settings") }
        }
    }
}

@Composable
private fun StatusPill(label: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = tint.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.25f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = tint.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun FavoritesShelf(
    apps: List<AppInfo>,
    onOpenApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit
) {
    val listState = rememberLazyListState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Favorites",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                val icon = rememberAppIcon(app.packageName)
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                                .homeAppInteraction(
                                    onClick = { onOpenApp(app) },
                                    onLongPress = { onLongPressApp(app) }
                                )
                        ) {
                            Image(bitmap = icon, contentDescription = app.label, modifier = Modifier.size(38.dp))
                        }
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeGridRow(
    apps: List<AppInfo>,
    onOpen: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        val slots = 3
        apps.forEach { app ->
            val icon = rememberAppIcon(app.packageName)
            Column(
                modifier = Modifier.weight(1f, fill = true),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .homeAppInteraction(
                            onClick = { onOpen(app) },
                            onLongPress = { onLongPress(app) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(bitmap = icon, contentDescription = app.label, modifier = Modifier.size(42.dp))
                }
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        repeat(slots - apps.size) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .height(74.dp)
            )
        }
    }
}

@Composable
private fun HomeDock(
    apps: List<AppInfo>,
    onOpen: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.take(5).forEach { app ->
                val icon = rememberAppIcon(app.packageName)
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .homeAppInteraction(
                            onClick = { onOpen(app) },
                            onLongPress = { onLongPress(app) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(bitmap = icon, contentDescription = app.label, modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorState(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun EmptyState(onRequestSetLauncher: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No apps in Nebula Home",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Choose your essentials to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Button(
                onClick = onRequestSetLauncher,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Pick apps", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
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

private val previewDockApps = listOf(
    AppInfo("Nebula Chat", "com.nebula.chat"),
    AppInfo("Orbital Studio", "com.nebula.studio"),
    AppInfo("Galaxy Maps", "com.nebula.maps"),
    AppInfo("Star Tunes", "com.nebula.music")
)

private val previewActiveUsage = mapOf(
    "com.nebula.chat" to com.example.launcherapp.domain.model.AppUsageSnapshot(
        packageName = "com.nebula.chat",
        limitMinutes = 90,
        usedMillisToday = 40 * 60 * 1000L
    ),
    "com.nebula.music" to com.example.launcherapp.domain.model.AppUsageSnapshot(
        packageName = "com.nebula.music",
        limitMinutes = null,
        usedMillisToday = 0
    )
)

private val previewActiveUiState = LauncherUiState(
    availableApps = previewDockApps,
    visibleApps = previewDockApps,
    allowedPackages = previewDockApps.map { it.packageName }.toSet(),
    selectionLocked = false,
    usageSnapshots = previewActiveUsage
)

@Preview(name = "Active Launcher", showBackground = true, backgroundColor = 0xFF050512)
@Composable
private fun ActiveLauncherScreenPreview() {
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
