package com.example.securekeep.ui.screens

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.securekeep.data.local.Note
import com.example.securekeep.ui.components.NoteCard
import com.example.securekeep.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedNotesScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    val deletedNotes by viewModel.deletedNotes.collectAsState(initial = emptyList())
    val context = LocalContext.current
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (deletedNotes.isNotEmpty()) {
                        IconButton(onClick = { showEmptyTrashDialog = true }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Empty Trash")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (deletedNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notes in Trash")
                }
            } else {
                Text(
                    "Notes in Trash are deleted after 30 days",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                ) {
                    items(deletedNotes) { note ->
                        NoteCard(
                            note = note,
                            onClick = {
                                selectedNote = note
                                showMenu = true
                            },
                            onLongClick = {
                                selectedNote = note
                                showMenu = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showMenu && selectedNote != null) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Note Options",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Restore") },
                    leadingContent = { Icon(Icons.Default.Restore, null) },
                    modifier = Modifier.clickable {
                        authenticateAction(context, "Restore Note") {
                            viewModel.restoreNote(selectedNote!!)
                            showMenu = false
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text("Delete permanently", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        authenticateAction(context, "Delete Permanently") {
                            viewModel.deletePermanently(selectedNote!!)
                            showMenu = false
                        }
                    }
                )
            }
        }
    }

    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash?") },
            text = { Text("All notes in Trash will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    authenticateAction(context, "Empty Trash") {
                        viewModel.emptyTrash()
                        showEmptyTrashDialog = false
                    }
                }) {
                    Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun authenticateAction(
    context: Context,
    action: String,
    onAuthenticated: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)
    val biometricPrompt = BiometricPrompt(
        context as FragmentActivity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onAuthenticated()
            }
        }
    )
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(action)
        .setSubtitle("Biometric authentication required")
        .setNegativeButtonText("Cancel")
        .build()
    biometricPrompt.authenticate(promptInfo)
}
