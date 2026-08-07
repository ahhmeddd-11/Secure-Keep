package com.example.securekeep.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.securekeep.viewmodel.DriveAuthViewModel
import com.example.securekeep.viewmodel.DriveSyncViewModel
import com.example.securekeep.viewmodel.NotesViewModel
import com.example.securekeep.viewmodel.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NotesViewModel,
    driveAuthViewModel: DriveAuthViewModel,
    driveSyncViewModel: DriveSyncViewModel,
    onBack: () -> Unit
) {

    val appPin by viewModel.appPin.collectAsState()
    val notePin by viewModel.notePin.collectAsState()
    val useBiometricApp by viewModel.useBiometricApp.collectAsState()
    val useBiometricNote by viewModel.useBiometricNote.collectAsState()

    val currentUser by driveAuthViewModel.currentUser.collectAsState()
    val loading by driveAuthViewModel.loading.collectAsState()

    val syncStatus by driveSyncViewModel.syncStatus.collectAsState()
    val lastBackupTime by driveSyncViewModel.lastBackupTime.collectAsState(initial = null)
    val pendingAuthIntent by driveSyncViewModel.pendingAuthIntent.collectAsState()

    val isSyncing = syncStatus is SyncStatus.BackingUp || syncStatus is SyncStatus.Restoring

    var showAppPinDialog by remember { mutableStateOf(false) }
    var showNotePinDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf<String?>(null) }
    var showFailureDialog by remember { mutableStateOf<String?>(null) }
    var backPressed by remember {
        mutableStateOf(false)
    }

    // Drive authorization activity launcher
    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            driveSyncViewModel.onAuthorizationCompleted()
        } else {
            driveSyncViewModel.onAuthorizationCancelled()
        }
    }

    // Launch Drive consent screen when a pending auth intent is emitted
    LaunchedEffect(pendingAuthIntent) {
        val intent = pendingAuthIntent ?: return@LaunchedEffect
        try {
            authLauncher.launch(
                IntentSenderRequest.Builder(intent.intentSender).build()
            )
        } catch (e: Exception) {
            driveSyncViewModel.onAuthorizationCancelled()
        }
    }

    // Observe sync completion to display result dialogs
    LaunchedEffect(syncStatus) {
        when (val status = syncStatus) {
            is SyncStatus.Success -> {
                showSuccessDialog = status.message
                driveSyncViewModel.clearStatus()
            }
            is SyncStatus.Failed -> {
                showFailureDialog = status.message
                driveSyncViewModel.clearStatus()
            }
            else -> { /* still in progress */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        enabled = !backPressed,
                        onClick = {
                            backPressed = true
                            onBack()
                        }
                    ) {
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

            //---------------------------------------------------------
            // SECURITY
            //---------------------------------------------------------

            SettingsSectionTitle(
                title = "Security",
                subtitle = "Manage your app protection",
                icon = Icons.Default.Security
            )


            CloudActionCard(
                title = "App PIN",
                subtitle = if (appPin == null)
                    "Not configured"
                else
                    "Change App PIN",
                icon = Icons.Default.Security,
                enabled = true,
                buttonText = "Open",
                onClick = {
                    showAppPinDialog = true
                }
            )

            CloudActionCard(
                title = "Note PIN",
                subtitle = if (notePin == null)
                    "Not configured"
                else
                    "Change Note PIN",
                icon = Icons.Default.Fingerprint,
                enabled = true,
                buttonText = "Open",
                onClick = {
                    showNotePinDialog = true
                }
            )

            BiometricCard(
                title = "Unlock App",
                subtitle = "Use biometrics to unlock SecureKeep",
                checked = useBiometricApp,
                onCheckedChange = {
                    viewModel.setUseBiometricApp(it)
                }
            )

            BiometricCard(
                title = "Unlock Notes",
                subtitle = "Use biometrics for locked notes",
                checked = useBiometricNote,
                onCheckedChange = {
                    viewModel.setUseBiometricNote(it)
                }
            )

            Spacer(Modifier.height(20.dp))

            HorizontalDivider()

            Spacer(Modifier.height(20.dp))

            //---------------------------------------------------------
            // CLOUD SYNC
            //---------------------------------------------------------

            SettingsSectionTitle(
                title = "Cloud Sync",
                subtitle = "Backup and restore your notes",
                icon = Icons.Default.Cloud
            )

            if (currentUser == null) {

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    onClick = { driveAuthViewModel.signIn() }
                ) {
                    Text(if (loading) "Signing in..." else "Sign in with Google")
                }

            } else {

                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDone, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Connected")
                                Text(
                                    currentUser!!.email,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (lastBackupTime != null)
                                        "Last backup: ${formatBackupTime(lastBackupTime!!)}"
                                    else
                                        "No backup yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSyncing,
                            onClick = {
                                driveAuthViewModel.signOut()
                                driveSyncViewModel.onUserSignedOut()
                            }
                        ) {
                            Text("Sign Out")
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            // ── Backup Card ──────────────────────────────────────────
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (syncStatus is SyncStatus.BackingUp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Backup Now",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (syncStatus is SyncStatus.BackingUp)
                                "Uploading encrypted notes…"
                            else
                                "Upload encrypted notes to Google Drive",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        enabled = currentUser != null && !isSyncing,
                        onClick = { driveSyncViewModel.backup() }
                    ) {
                        Text("Backup")
                    }
                }
            }

            // ── Restore Card ─────────────────────────────────────────
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (syncStatus is SyncStatus.Restoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Restore Notes",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (syncStatus is SyncStatus.Restoring)
                                "Downloading and merging notes…"
                            else
                                "Download notes from Google Drive",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        enabled = currentUser != null && !isSyncing,
                        onClick = { driveSyncViewModel.restore() }
                    ) {
                        Text("Restore")
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            HorizontalDivider()

            Spacer(Modifier.height(20.dp))

            //---------------------------------------------------------
            // ABOUT
            //---------------------------------------------------------

            SettingsSectionTitle(
                title = "About",
                subtitle = "Application information",
                icon = Icons.Default.Info
            )

            Spacer(Modifier.height(12.dp))

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "SecureKeep",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Version 1.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Built with ❤️ by",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Syed Ahmed Ali",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Thank you for using SecureKeep",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }

    }

    // ── Success dialog ────────────────────────────────────────────────
    showSuccessDialog?.let { message ->
        AlertDialog(
            onDismissRequest = { showSuccessDialog = null },
            title = { Text("Success") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = null }) { Text("OK") }
            }
        )
    }

    // ── Failure dialog ────────────────────────────────────────────────
    showFailureDialog?.let { message ->
        AlertDialog(
            onDismissRequest = { showFailureDialog = null },
            title = { Text("Sync Failed") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { showFailureDialog = null }) { Text("OK") }
            }
        )
    }

    // ── PIN dialogs (unchanged) ───────────────────────────────────────

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

private fun formatBackupTime(epochMs: Long): String {
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return formatter.format(Date(epochMs))
}

@Composable
fun SettingsSectionTitle(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.width(12.dp))

        Column {

            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun CloudActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Button(enabled = enabled, onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun BiometricCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null
            )

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
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
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                when (step) {
                    0 -> {
                        if (prevPinInput == currentPin) { step = 1; error = null }
                        else error = "Incorrect previous PIN"
                    }
                    1 -> {
                        if (newPinInput.length == 6) { step = 2; error = null }
                        else error = "PIN must be 6 digits"
                    }
                    2 -> {
                        if (confirmPinInput == newPinInput) { onSave(newPinInput); onDismiss() }
                        else error = "PINs do not match"
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
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
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
                        } else error = "Incorrect App PIN"
                    }
                    1 -> {
                        if (currentNotePinInput == currentNotePin) { step = 2; error = null }
                        else error = "Incorrect Current Note PIN"
                    }
                    2 -> {
                        if (newPinInput.length == 6) { step = 3; error = null }
                        else error = "PIN must be 6 digits"
                    }
                    3 -> {
                        if (confirmPinInput == newPinInput) { onSave(newPinInput); onDismiss() }
                        else error = "PINs do not match"
                    }
                }
            }) {
                Text(if (step == 3) "Save" else "Next")
            }
        }
    )
}