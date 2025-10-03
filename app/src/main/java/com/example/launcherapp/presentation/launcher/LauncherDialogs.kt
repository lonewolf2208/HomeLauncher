package com.example.launcherapp.presentation.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.launcherapp.domain.model.AppInfo

@Composable
fun PasswordDialog(
    error: String?,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock Launcher") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the password to leave this launcher.")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    label = { Text("Password") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(password) }, enabled = password.isNotBlank()) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AppSelectionDialog(
    apps: List<AppInfo>,
    initialSelection: Set<String>,
    initialLimits: Map<String, Int?>,
    onConfirm: (Set<String>, Map<String, Int?>) -> Unit,
    onDismiss: () -> Unit
) {
    var selection by remember { mutableStateOf(initialSelection) }
    val limitInputs = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(initialSelection, initialLimits) {
        selection = initialSelection
        limitInputs.clear()
        initialLimits.forEach { (pkg, limit) ->
            if (limit != null) {
                limitInputs[pkg] = limit.toString()
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Apps") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pick the apps that should appear in this launcher.",
                    style = MaterialTheme.typography.bodyMedium
                )
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    val dialogListState = rememberLazyListState()
                    LazyColumn(state = dialogListState) {
                        items(apps, key = { it.packageName }) { app ->
                            val isChecked = selection.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .toggleable(
                                        value = isChecked,
                                        onValueChange = { checked ->
                                            selection = (if (checked) {
                                                selection + app.packageName
                                            } else {
                                                selection - app.packageName
                                                limitInputs.remove(app.packageName)
                                            }) as Set<String>
                                        },
                                        role = Role.Checkbox
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = null
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(app.label, style = MaterialTheme.typography.bodyLarge)
                                    if (isChecked) {
                                        OutlinedTextField(
                                            value = limitInputs[app.packageName] ?: "",
                                            onValueChange = { value ->
                                                limitInputs[app.packageName] = value.filter { it.isDigit() }.take(4)
                                            },
                                            singleLine = true,
                                            label = { Text("Daily limit (min)") },
                                            placeholder = { Text("Unlimited") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                        Text(
                                            app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    } else {
                                        Text(
                                            app.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limits = selection.associateWith { pkg ->
                        limitInputs[pkg]?.takeIf { it.isNotBlank() }?.toIntOrNull()
                    }
                    onConfirm(selection, limits)
                },
                enabled = selection.isNotEmpty()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
