package com.example.launcherapp.presentation.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.launcherapp.domain.model.AppInfo
import com.example.launcherapp.domain.model.AppUsageSnapshot
import com.example.launcherapp.drawableToImageBitmap

@Composable
fun LauncherHeader(timeText: String, appCount: Int, isHomeLauncher: Boolean) {
    val titleColor = if (isHomeLauncher) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
    }
    val subtitleColor = if (isHomeLauncher) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
    }
    val statusText = if (isHomeLauncher) {
        if (appCount > 0) "$appCount apps in orbit" else "Scanning installed apps…"
    } else {
        "Previewing ${if (appCount > 0) "$appCount" else "0"} curated apps"
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = if (isHomeLauncher) "Nebula Home" else "Nebula Home · Preview",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = titleColor
        )
        Text(
            text = timeText,
            style = MaterialTheme.typography.titleMedium,
            color = subtitleColor
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun StatusBanner(isHomeLauncher: Boolean) {
    val label: String
    val description: String
    val borderColor: Color
    val backgroundColor: Color
    val textColor: Color

    if (isHomeLauncher) {
        label = "Active Launcher"
        description = "Nebula Home currently powers your device navigation."
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        textColor = MaterialTheme.colorScheme.primary
    } else {
        label = "Preview Mode"
        description = "Set Nebula Home as default to lock the universe in place."
        borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
        textColor = MaterialTheme.colorScheme.secondary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
fun NeonSearchBar(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Search the verse",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                )
                Text(
                    text = "Hint: long-press any tile for details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            TextButton(onClick = {}, enabled = false) {
                Text("Coming soon")
            }
        }
    }
}

@Composable
fun QuickActionRow(
    selectionLocked: Boolean,
    onSetLauncher: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val scrollState = rememberScrollState()
        val quickActionsState = rememberLazyListState()
        LazyRow(
            modifier = Modifier.weight(1f),
            state = quickActionsState,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(4) { index ->
                when (index) {
                    0 -> QuickActionChip(
                        label = "Choose apps",
                        onClick = onSetLauncher,
                        enabled = !selectionLocked
                    )
                    1 -> QuickActionChip(
                        label = "Home settings",
                        onClick = onOpenDefaultSettings
                    )
                    2 -> QuickActionChip(
                        label = "System settings",
                        onClick = onOpenSystemSettings
                    )
                    else -> QuickActionChip(
                        label = if (selectionLocked) "Locked" else "Unlocked",
                        onClick = {},
                        enabled = false
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionChip(label = "Widgets", onClick = {}, enabled = false)
            QuickActionChip(label = "Themes", onClick = {}, enabled = false)
        }
    }
}

@Composable
private fun QuickActionChip(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        enabled = enabled,
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 0.6f else 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun StylizedGridCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(40.dp),
        tonalElevation = 18.dp,
        shadowElevation = 22.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                    )
                )
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGrid(
    apps: List<AppInfo>,
    usageSnapshots: Map<String, AppUsageSnapshot>,
    onOpen: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val pm = LocalContext.current.packageManager
    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = modifier,
        state = gridState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        itemsIndexed(apps, key = { _, info -> info.packageName }) { index, appInfo ->
            val icon = remember(appInfo.packageName) {
                runCatching {
                    val drawable = pm.getApplicationIcon(appInfo.packageName)
                    drawableToImageBitmap(drawable)
                }.getOrElse {
                    drawableToImageBitmap(pm.defaultActivityIcon)
                }
            }
            AppTile(
                label = appInfo.label,
                packageName = appInfo.packageName,
                icon = icon,
                index = index,
                accent = MaterialTheme.colorScheme.primary,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                usageSnapshot = usageSnapshots[appInfo.packageName],
                onClick = { onOpen(appInfo) },
                onLongPress = { onLongPress(appInfo) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    label: String,
    packageName: String,
    icon: ImageBitmap,
    index: Int,
    accent: Color,
    backgroundColor: Color,
    usageSnapshot: AppUsageSnapshot?,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val verticalOffset = remember(index) { ((index % 3) - 1) * 8 }
    val rotation = remember(index) { ((index % 4) - 1.5f) * 2.8f }
    Column(
        modifier = Modifier
            .offset(y = verticalOffset.dp)
            .graphicsLayer { rotationZ = rotation }
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.9f),
                        backgroundColor.copy(alpha = 0.35f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.5f),
                        accent.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f))
                .border(1.dp, accent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = icon,
                contentDescription = label,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.size(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.size(4.dp))
        if (usageSnapshot?.limitMinutes != null) {
            val remaining = usageSnapshot.remainingMinutes ?: 0
            Text(
                text = "$remaining min left",
                style = MaterialTheme.typography.bodySmall,
                color = if (remaining <= 5) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                } else {
                    accent.copy(alpha = 0.8f)
                }
            )
        } else {
            Text(
                text = "Unlimited",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Text(
            text = packageName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = "Long press for details",
            style = MaterialTheme.typography.labelSmall,
            color = accent.copy(alpha = 0.7f)
        )
    }
}
