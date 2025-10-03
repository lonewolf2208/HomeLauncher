package com.example.launcherapp.presentation.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.launcherapp.ui.theme.LauncherAppTheme
import kotlinx.coroutines.delay
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
    LauncherAppTheme(darkTheme = true) {
        var currentTime by remember { mutableStateOf(Date()) }
        val timeFormatter = remember { SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()) }

        LaunchedEffect(Unit) {
            while (true) {
                currentTime = Date()
                delay(60_000)
            }
        }

        val backgroundGradient = if (isHomeLauncher) {
            Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                    Color(0xFF050512)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF120718),
                    Color(0xFF23113A),
                    Color(0xFF1B1E2E)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
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
}
