package com.example.launcherapp.presentation.launcher

import android.app.WallpaperManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.launcherapp.drawableToImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LauncherScreenScaffold(
    isHomeLauncher: Boolean,
    showPasswordPrompt: Boolean,
    passwordError: String?,
    onAttemptUnlock: (String) -> Unit,
    onCancelUnlock: () -> Unit,
    content: @Composable (currentTime: Date, currentTimeText: String) -> Unit
) {
    var currentTime by remember { mutableStateOf(Date()) }
    val timeFormatter = remember { SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(60_000)
        }
    }

    val context = LocalContext.current
//    val wallpaperImage by produceState<ImageBitmap?>(initialValue = null, key1 = context) {
//        val bitmap = withContext(Dispatchers.IO) {
//            runCatching {
//                WallpaperManager.getInstance(context).drawable?.let { drawable ->
//                    drawableToImageBitmap(drawable)
//                }
//            }.getOrNull()
//        }
//        value = bitmap
//    }

    val overlayBrush = if (isHomeLauncher) {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                Color(0xFF050512)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xC0120718),
                Color(0xC023113A),
                Color(0xC01B1E2E)
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
//        if (wallpaperImage != null) {
//            Image(
//                bitmap = wallpaperImage,
//                contentDescription = null,
//                modifier = Modifier
//                    .fillMaxSize()
//                    .blur(28.dp)
//                    .alpha(0.9f),
//                contentScale = ContentScale.Crop
//            )
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(overlayBrush)
//            )
//        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBrush)
            )
//        }

        content(currentTime, timeFormatter.format(currentTime))

        if (showPasswordPrompt) {
            PasswordDialog(
                error = passwordError,
                onSubmit = onAttemptUnlock,
                onDismiss = onCancelUnlock
            )
        }
    }
}
