package com.example.securekeep.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.securekeep.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    val appPin by viewModel.appPin.collectAsState()
    val notePin by viewModel.notePin.collectAsState()
    val useBiometricApp by viewModel.useBiometricApp.collectAsState()
    val useBiometricNote by viewModel.useBiometricNote.collectAsState()

    var showAppPinDialog by remember { mutableStateOf(false) }
    var showNotePinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSectionTitle(title = "App Lock", icon = Icons.Default.Lock)
            SettingsItem(
                title = if (appPin == null) "Set App PIN" else "Change App PIN",
                subtitle = "6-digit PIN to secure the app",
                onClick = { showAppPinDialog = true }
            )
            
            ListItem(
                headlineContent = { Text("Biometrics for App") },
                supportingContent = { Text("Unlock app using fingerprint") },
                leadingContent = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = useBiometricApp,
                        onCheckedChange = { viewModel.setUseBiometricApp(it) },
                        enabled = appPin != null
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionTitle(title = "Notes Lock", icon = Icons.AutoMirrored.Filled.Note)
            SettingsItem(
                title = if (notePin == null) "Set Note PIN" else "Change Note PIN",
                subtitle = "Separate PIN for locking individual notes",
                onClick = { showNotePinDialog = true }
            )
            
            ListItem(
                headlineContent = { Text("Biometrics for Notes") },
                supportingContent = { Text("Unlock notes using fingerprint") },
                leadingContent = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = useBiometricNote,
                        onCheckedChange = { viewModel.setUseBiometricNote(it) },
                        enabled = notePin != null
                    )
                }
            )
        }
    }

    if (showAppPinDialog) {
        AppPinManagementDialog(
            currentPin = appPin,
            onDismiss = { showAppPinDialog = false },
            onSave = { viewModel.saveAppPin(it) }
        )
    }

    if (showNotePinDialog) {
        NotePinManagementDialog(
            appPin = appPin,
            currentNotePin = notePin,
            onDismiss = { showNotePinDialog = false },
            onSave = { viewModel.saveNotePin(it) }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AppPinManagementDialog(
    currentPin: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var step by remember { mutableStateOf(if (currentPin == null) 1 else 0) }
    var prevPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (step) {
                    0 -> "Enter Previous PIN"
                    1 -> "Enter New 6-digit PIN"
                    else -> "Confirm New PIN"
                }
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = when (step) {
                        0 -> prevPinInput
                        1 -> newPinInput
                        else -> confirmPinInput
                    },
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            when (step) {
                                0 -> prevPinInput = it
                                1 -> newPinInput = it
                                else -> confirmPinInput = it
                            }
                            error = null
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                when (step) {
                    0 -> {
                        if (prevPinInput == currentPin) {
                            step = 1
                            error = null
                        } else {
                            error = "Incorrect previous PIN"
                        }
                    }
                    1 -> {
                        if (newPinInput.length == 6) {
                            step = 2
                            error = null
                        } else {
                            error = "PIN must be 6 digits"
                        }
                    }
                    2 -> {
                        if (confirmPinInput == newPinInput) {
                            onSave(newPinInput)
                            onDismiss()
                        } else {
                            error = "PINs do not match"
                        }
                    }
                }
            }) {
                Text(if (step == 2) "Save" else "Next")
            }
        }
    )
}

@Composable
fun NotePinManagementDialog(
    appPin: String?,
    currentNotePin: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var appPinInput by remember { mutableStateOf("") }
    var currentNotePinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (step) {
                    0 -> "Enter App PIN first"
                    1 -> "Enter Current Note PIN"
                    2 -> "Enter New Note PIN"
                    else -> "Confirm New Note PIN"
                }
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = when (step) {
                        0 -> appPinInput
                        1 -> currentNotePinInput
                        2 -> newPinInput
                        else -> confirmPinInput
                    },
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            when (step) {
                                0 -> appPinInput = it
                                1 -> currentNotePinInput = it
                                2 -> newPinInput = it
                                else -> confirmPinInput = it
                            }
                            error = null
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                when (step) {
                    0 -> {
                        if (appPin == null || appPinInput == appPin) {
                            step = if (currentNotePin == null) 2 else 1
                            error = null
                        } else {
                            error = "Incorrect App PIN"
                        }
                    }
                    1 -> {
                        if (currentNotePinInput == currentNotePin) {
                            step = 2
                            error = null
                        } else {
                            error = "Incorrect Current Note PIN"
                        }
                    }
                    2 -> {
                        if (newPinInput.length == 6) {
                            step = 3
                            error = null
                        } else {
                            error = "PIN must be 6 digits"
                        }
                    }
                    3 -> {
                        if (confirmPinInput == newPinInput) {
                            onSave(newPinInput)
                            onDismiss()
                        } else {
                            error = "PINs do not match"
                        }
                    }
                }
            }) {
                Text(if (step == 3) "Save" else "Next")
            }
        }
    )
}
