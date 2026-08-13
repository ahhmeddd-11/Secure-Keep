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
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import com.example.securekeep.ui.components.PinInputField
import com.example.securekeep.ui.components.PinVerificationState

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

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column {

                    // App PIN
                    Surface(
                        onClick = {
                            showAppPinDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "App PIN",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = if (appPin == null)
                                        "Not configured"
                                    else
                                        "Enabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "›",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    // Note PIN
                    Surface(
                        onClick = {
                            showNotePinDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
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
                                    text = "Note PIN",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = if (notePin == null)
                                        "Not configured"
                                    else
                                        "Enabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "›",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    // App Biometrics
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
                                text = "Unlock App with Biometrics",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "Use fingerprint to unlock SecureKeep",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = appPin != null && useBiometricApp,
                            enabled = appPin != null,
                            onCheckedChange = {
                                viewModel.setUseBiometricApp(it)
                            }
                        )
                    }

                    HorizontalDivider()

                    // Note Biometrics
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
                                text = "Unlock Notes with Biometrics",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "Use fingerprint for locked notes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = notePin != null && useBiometricNote,
                            enabled = notePin != null,
                            onCheckedChange = {
                                viewModel.setUseBiometricNote(it)
                            }
                        )
                    }
                }
            }

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

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column {

                    // -------------------------------------------------
                    // GOOGLE DRIVE CONNECTION
                    // -------------------------------------------------

                    if (currentUser == null) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null
                                )

                                Spacer(
                                    modifier = Modifier.width(16.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = "Google Drive",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        text = "Connect your Google account to backup and restore your notes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loading,
                                onClick = {
                                    driveAuthViewModel.signIn()
                                }
                            ) {
                                Text(
                                    if (loading)
                                        "Signing in..."
                                    else
                                        "Sign in with Google"
                                )
                            }
                        }

                    } else {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null
                                )

                                Spacer(
                                    modifier = Modifier.width(16.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = "Connected",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        text = currentUser!!.email,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        text = if (lastBackupTime != null)
                                            "Last backup: ${
                                                formatBackupTime(lastBackupTime!!)
                                            }"
                                        else
                                            "No backup yet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

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

                    HorizontalDivider()

                    // -------------------------------------------------
                    // BACKUP
                    // -------------------------------------------------

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

                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(16.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Backup Now",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = if (syncStatus is SyncStatus.BackingUp)
                                    "Uploading encrypted notes…"
                                else
                                    "Upload encrypted notes to Google Drive",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            enabled = currentUser != null && !isSyncing,
                            onClick = {
                                driveSyncViewModel.backup()
                            }
                        ) {
                            Text("Backup")
                        }
                    }

                    HorizontalDivider()

                    // -------------------------------------------------
                    // RESTORE
                    // -------------------------------------------------

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

                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(16.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Restore Notes",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = if (syncStatus is SyncStatus.Restoring)
                                    "Downloading and merging notes…"
                                else
                                    "Download notes from Google Drive",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            enabled = currentUser != null && !isSyncing,
                            onClick = {
                                driveSyncViewModel.restore()
                            }
                        ) {
                            Text("Restore")
                        }
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
            notePin = notePin,
            onDismiss = { showAppPinDialog = false },
            onSave = {
                viewModel.saveAppPin(it)

                if (appPin == null) {
                    viewModel.setUseBiometricApp(true)
                }
            },
            onDisable = { viewModel.clearAppPin() }
        )
    }

    if (showNotePinDialog) {
        NotePinManagementDialog(
            appPin = appPin,
            currentNotePin = notePin,
            onDismiss = { showNotePinDialog = false },
            onSave = {
                viewModel.saveNotePin(it)

                if (notePin == null) {
                    viewModel.setUseBiometricNote(true)
                }
            },
            onDisable = { viewModel.clearNotePin() }
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
    notePin: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDisable: () -> Unit
) {
    var showChangePin by remember { mutableStateOf(false) }
    var showDisablePin by remember { mutableStateOf(false) }
    var showNotePinRequiredDialog by remember { mutableStateOf(false) }

    if (showChangePin) {
        AppPinChangeDialog(
            currentPin = currentPin,
            onDismiss = {
                showChangePin = false
            },
            onSave = {
                onSave(it)
                onDismiss()
            }
        )
        return
    }

    if (showDisablePin) {
        DisableAppPinDialog(
            currentPin = currentPin,
            onDismiss = {
                showDisablePin = false
            },
            onDisable = {
                onDisable()
                onDismiss()
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "App PIN",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {

                Text(
                    text = if (currentPin == null)
                        "App PIN protection is currently disabled."
                    else
                        "Your app is protected with a 6-digit PIN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                if (currentPin != null) {

                    ListItem(
                        headlineContent = {
                            Text("Change App PIN")
                        },
                        trailingContent = {
                            Text(
                                text = "›",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.clickable {
                            showChangePin = true
                        }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = {
                            Text(
                                text = "Disable App PIN",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        trailingContent = {
                            Text(
                                text = "›",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.clickable {
                            if (notePin != null) {
                                showNotePinRequiredDialog = true
                            } else {
                                showDisablePin = true
                            }
                        }
                    )

                } else {

                    ListItem(
                        headlineContent = {
                            Text("Set App PIN")
                        },
                        trailingContent = {
                            Text(
                                text = "›",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.clickable {
                            showChangePin = true
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )

    if (showNotePinRequiredDialog) {
        AlertDialog(
            onDismissRequest = {
                showNotePinRequiredDialog = false
            },
            title = {
                Text("Cannot Disable App PIN")
            },
            text = {
                Text("Please disable Note PIN first.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotePinRequiredDialog = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun AppPinChangeDialog(
    currentPin: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var step by remember {
        mutableStateOf(if (currentPin == null) 1 else 0)
    }

    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }

    var error by remember { mutableStateOf<String?>(null) }

    var verificationState by remember {
        mutableStateOf(PinVerificationState.NORMAL)
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                when (step) {
                    0 -> "Enter Current App PIN"
                    1 -> "Enter New 6-digit PIN"
                    else -> "Confirm New PIN"
                }
            )
        },

        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                PinInputField(
                    value = when (step) {
                        0 -> currentPinInput
                        1 -> newPinInput
                        else -> confirmPinInput
                    },

                    onValueChange = { value ->

                        when (step) {
                            0 -> currentPinInput = value
                            1 -> newPinInput = value
                            else -> confirmPinInput = value
                        }

                        error = null

                        if (
                            verificationState ==
                            PinVerificationState.ERROR
                        ) {
                            verificationState =
                                PinVerificationState.NORMAL
                        }
                    },

                    verificationState = verificationState,

                    onComplete = {

                        when (step) {

                            // Verify current App PIN
                            0 -> {
                                if (currentPinInput == currentPin) {

                                    verificationState =
                                        PinVerificationState.SUCCESS

                                } else {

                                    error =
                                        "Incorrect current App PIN"

                                    verificationState =
                                        PinVerificationState.ERROR
                                }
                            }

                            // New PIN only needs to contain 6 digits
                            1 -> {
                                verificationState =
                                    PinVerificationState.SUCCESS
                            }

                            // Confirm new PIN
                            2 -> {
                                if (confirmPinInput == newPinInput) {

                                    verificationState =
                                        PinVerificationState.SUCCESS

                                } else {

                                    error = "PINs do not match"

                                    verificationState =
                                        PinVerificationState.ERROR
                                }
                            }
                        }
                    },

                    onSuccessAnimationFinished = {

                        when (step) {

                            0 -> {
                                currentPinInput = ""
                                error = null

                                verificationState =
                                    PinVerificationState.NORMAL

                                step = 1
                            }

                            1 -> {
                                error = null

                                verificationState =
                                    PinVerificationState.NORMAL

                                confirmPinInput = ""
                                step = 2
                            }

                            2 -> {
                                onSave(newPinInput)
                                onDismiss()
                            }
                        }
                    },

                    onErrorAnimationFinished = {

                        when (step) {

                            0 -> {
                                currentPinInput = ""
                            }

                            1 -> {
                                newPinInput = ""
                            }

                            2 -> {
                                // Confirmation failed.
                                // Clear both PIN fields and return to
                                // the new PIN entry screen.
                                newPinInput = ""
                                confirmPinInput = ""
                                step = 1
                            }
                        }

                        error = null

                        verificationState =
                            PinVerificationState.NORMAL
                    }
                )

                if (error != null) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },

        confirmButton = {},

        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DisableAppPinDialog(
    currentPin: String?,
    onDismiss: () -> Unit,
    onDisable: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    var verificationState by remember {
        mutableStateOf(PinVerificationState.NORMAL)
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Disable App PIN")
        },

        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "Enter your current App PIN to disable protection."
                )

                Spacer(Modifier.height(16.dp))

                PinInputField(
                    value = pinInput,

                    onValueChange = {
                        pinInput = it
                        error = null

                        if (
                            verificationState ==
                            PinVerificationState.ERROR
                        ) {
                            verificationState =
                                PinVerificationState.NORMAL
                        }
                    },

                    verificationState = verificationState,

                    onComplete = {

                        if (pinInput == currentPin) {

                            verificationState =
                                PinVerificationState.SUCCESS

                        } else {

                            error = "Incorrect App PIN"

                            verificationState =
                                PinVerificationState.ERROR
                        }
                    },

                    onSuccessAnimationFinished = {
                        onDisable()
                        onDismiss()
                    },

                    onErrorAnimationFinished = {
                        pinInput = ""
                        error = null

                        verificationState =
                            PinVerificationState.NORMAL
                    }
                )

                if (error != null) {

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },

        confirmButton = {},

        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NotePinManagementDialog(
    appPin: String?,
    currentNotePin: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDisable: () -> Unit
) {
    var showChangePin by remember { mutableStateOf(false) }
    var showDisablePin by remember { mutableStateOf(false) }

    if (showChangePin) {
        NotePinChangeDialog(
            appPin = appPin,
            currentNotePin = currentNotePin,
            onDismiss = {
                showChangePin = false
            },
            onSave = {
                onSave(it)
                onDismiss()
            }
        )
        return
    }

    if (showDisablePin) {
        DisableNotePinDialog(
            appPin = appPin,
            currentNotePin = currentNotePin,
            onDismiss = {
                showDisablePin = false
            },
            onDisable = {
                onDisable()
                onDismiss()
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Note PIN",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {

                Text(
                    text = if (currentNotePin == null)
                        "Note PIN protection is currently disabled."
                    else
                        "Your locked notes are protected with a 6-digit PIN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                if (currentNotePin != null) {

                    ListItem(
                        headlineContent = {
                            Text("Change Note PIN")
                        },
                        trailingContent = {
                            Text(
                                text = "›",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.clickable {
                            showChangePin = true
                        }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = {
                            Text(
                                text = "Disable Note PIN",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        trailingContent = {
                            Text(
                                text = "›",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.clickable {
                            showDisablePin = true
                        }
                    )

                } else {

                    ListItem(
                        headlineContent = {
                            Text("Set Note PIN")
                        },
                        trailingContent = {
                            Text(
                                text = "›",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.clickable {
                            showChangePin = true
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NotePinChangeDialog(
    appPin: String?,
    currentNotePin: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var step by remember {
        mutableStateOf(
            when {
                appPin != null -> 0
                currentNotePin != null -> 1
                else -> 2
            }
        )
    }

    var appPinInput by remember { mutableStateOf("") }
    var currentNotePinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }

    var error by remember { mutableStateOf<String?>(null) }

    var verificationState by remember {
        mutableStateOf(PinVerificationState.NORMAL)
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                when (step) {
                    0 -> "Enter App PIN"
                    1 -> "Enter Current Note PIN"
                    2 -> "Enter New 6-digit PIN"
                    else -> "Confirm New Note PIN"
                }
            )
        },

        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                PinInputField(
                    value = when (step) {
                        0 -> appPinInput
                        1 -> currentNotePinInput
                        2 -> newPinInput
                        else -> confirmPinInput
                    },

                    onValueChange = { value ->

                        when (step) {
                            0 -> appPinInput = value
                            1 -> currentNotePinInput = value
                            2 -> newPinInput = value
                            else -> confirmPinInput = value
                        }

                        error = null

                        if (
                            verificationState ==
                            PinVerificationState.ERROR
                        ) {
                            verificationState =
                                PinVerificationState.NORMAL
                        }
                    },

                    verificationState = verificationState,

                    onComplete = {

                        when (step) {

                            // Verify App PIN
                            0 -> {
                                if (appPin == null ||
                                    appPinInput == appPin
                                ) {
                                    verificationState =
                                        PinVerificationState.SUCCESS
                                } else {
                                    error = "Incorrect App PIN"

                                    verificationState =
                                        PinVerificationState.ERROR
                                }
                            }

                            // Verify existing Note PIN
                            1 -> {
                                if (
                                    currentNotePinInput ==
                                    currentNotePin
                                ) {
                                    verificationState =
                                        PinVerificationState.SUCCESS
                                } else {
                                    error =
                                        "Incorrect Current Note PIN"

                                    verificationState =
                                        PinVerificationState.ERROR
                                }
                            }

                            // New PIN only needs six digits
                            2 -> {
                                verificationState =
                                    PinVerificationState.SUCCESS
                            }

                            // Confirm new PIN
                            3 -> {
                                if (
                                    confirmPinInput ==
                                    newPinInput
                                ) {
                                    verificationState =
                                        PinVerificationState.SUCCESS
                                } else {
                                    error = "PINs do not match"

                                    verificationState =
                                        PinVerificationState.ERROR
                                }
                            }
                        }
                    },

                    onSuccessAnimationFinished = {

                        when (step) {

                            0 -> {
                                appPinInput = ""
                                error = null
                                verificationState =
                                    PinVerificationState.NORMAL

                                step = if (currentNotePin == null) {
                                    2
                                } else {
                                    1
                                }
                            }

                            1 -> {
                                currentNotePinInput = ""
                                error = null
                                verificationState =
                                    PinVerificationState.NORMAL

                                step = 2
                            }

                            2 -> {
                                error = null
                                verificationState =
                                    PinVerificationState.NORMAL

                                confirmPinInput = ""
                                step = 3
                            }

                            3 -> {
                                onSave(newPinInput)
                                onDismiss()
                            }
                        }
                    },

                    onErrorAnimationFinished = {

                        when (step) {

                            0 -> {
                                appPinInput = ""
                            }

                            1 -> {
                                currentNotePinInput = ""
                            }

                            2 -> {
                                newPinInput = ""
                            }

                            3 -> {
                                // Confirmation failed.
                                // Clear both PIN fields and return to
                                // the new Note PIN entry screen.
                                newPinInput = ""
                                confirmPinInput = ""
                                step = 2
                            }
                        }

                        error = null

                        verificationState =
                            PinVerificationState.NORMAL
                    }
                )

                if (error != null) {

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },

        confirmButton = {},

        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DisableNotePinDialog(
    appPin: String?,
    currentNotePin: String?,
    onDismiss: () -> Unit,
    onDisable: () -> Unit
) {
    var step by remember {
        mutableStateOf(if (appPin != null) 0 else 1)
    }

    var appPinInput by remember { mutableStateOf("") }
    var notePinInput by remember { mutableStateOf("") }

    var error by remember { mutableStateOf<String?>(null) }

    var verificationState by remember {
        mutableStateOf(PinVerificationState.NORMAL)
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                if (step == 0)
                    "Enter App PIN"
                else
                    "Enter Current Note PIN"
            )
        },

        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = if (step == 0)
                        "Enter your App PIN to continue."
                    else
                        "Enter your current Note PIN to disable protection.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(16.dp))

                PinInputField(
                    value = if (step == 0)
                        appPinInput
                    else
                        notePinInput,

                    onValueChange = { value ->

                        if (step == 0) {
                            appPinInput = value
                        } else {
                            notePinInput = value
                        }

                        error = null

                        if (
                            verificationState ==
                            PinVerificationState.ERROR
                        ) {
                            verificationState =
                                PinVerificationState.NORMAL
                        }
                    },

                    verificationState = verificationState,

                    onComplete = {

                        if (step == 0) {

                            if (appPinInput == appPin) {

                                verificationState =
                                    PinVerificationState.SUCCESS

                            } else {

                                error = "Incorrect App PIN"

                                verificationState =
                                    PinVerificationState.ERROR
                            }

                        } else {

                            if (notePinInput == currentNotePin) {

                                verificationState =
                                    PinVerificationState.SUCCESS

                            } else {

                                error = "Incorrect Note PIN"

                                verificationState =
                                    PinVerificationState.ERROR
                            }
                        }
                    },

                    onSuccessAnimationFinished = {

                        if (step == 0) {

                            appPinInput = ""
                            error = null

                            verificationState =
                                PinVerificationState.NORMAL

                            step = 1

                        } else {

                            onDisable()
                            onDismiss()
                        }
                    },

                    onErrorAnimationFinished = {

                        if (step == 0) {
                            appPinInput = ""
                        } else {
                            notePinInput = ""
                        }

                        error = null

                        verificationState =
                            PinVerificationState.NORMAL
                    }
                )

                if (error != null) {

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },

        confirmButton = {},

        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}