package com.example.securekeep.ui.screens

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import com.example.securekeep.data.local.Note
import com.example.securekeep.ui.components.NoteCard
import com.example.securekeep.ui.components.NotesSearchBar
import com.example.securekeep.viewmodel.NotesViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onAddClick: () -> Unit,
    onNoteClick: (Int, String?) -> Unit,
    onSettingsClick: () -> Unit,
    onDeletedNotesClick: () -> Unit
) {
    val notes by viewModel.allNotes.collectAsState(initial = emptyList())
    val notePin by viewModel.notePin.collectAsState()
    val useBiometricNote by viewModel.useBiometricNote.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun exitSearchMode() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showNotePinDialog by remember { mutableStateOf(false) }
    var notePinInput by remember { mutableStateOf("") }
    var notePinError by remember { mutableStateOf<String?>(null) }

    var isUnlockingForToggle by remember { mutableStateOf(false) }
    var isUnlockingForRename by remember { mutableStateOf(false) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var newLockedName by remember { mutableStateOf("") }


    val filteredNotes = notes.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.7f)) {
                Text(
                    "Secure Keep",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    label = { Text("Deleted notes") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                        }
                        exitSearchMode()
                        onDeletedNotesClick()
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                        }
                        exitSearchMode()
                        onSettingsClick()
                    }
                )
                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Built with ❤️ by",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "Syed Ahmed Ali",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(16.dp))

                    HorizontalDivider()

                    Spacer(Modifier.height(8.dp))
                }

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            if (isDarkMode == true)
                                Icons.Default.LightMode
                            else
                                Icons.Default.DarkMode,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            if (isDarkMode == true)
                                "Light Mode"
                            else
                                "Dark Mode"
                        )
                    },
                    selected = false,
                    onClick = {
                        viewModel.setDarkMode(!(isDarkMode ?: false))
                    },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    NotesSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onMenuClick = {
                            exitSearchMode()

                            scope.launch {
                                drawerState.open()
                            }
                        },
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        exitSearchMode()
                        onAddClick()
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note", modifier = Modifier.size(28.dp))
                }
            }
        ) { padding ->
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "No notes yet" else "No matching notes found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    exitSearchMode()
                                }
                            )
                        }
                ) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = {
                                    selectedNote = note
                                    if (note.isLocked) {
                                        isUnlockingForToggle = false
                                        isUnlockingForRename = false
                                        authenticateNote(context, onAuthenticated = {
                                            exitSearchMode()
                                            onNoteClick(note.id, searchQuery)
                                        }, onPinRequired = {
                                            showNotePinDialog = true
                                        })
                                    } else {
                                        exitSearchMode()
                                        onNoteClick(note.id, searchQuery)
                                    }
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
    }

    if (showMenu && selectedNote != null) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            confirmButton = {},
            title = { Text("Options") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            authenticateAction(context, "Move to Trash") {
                                viewModel.moveToTrash(selectedNote!!)
                                showMenu = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Note", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(
                        onClick = {
                            if (selectedNote!!.isLocked) {
                                if (notePin == null) {
                                    exitSearchMode()
                                    onSettingsClick()
                                } else {
                                    isUnlockingForToggle = true
                                    isUnlockingForRename = false
                                    authenticateNote(context, onAuthenticated = {
                                        viewModel.toggleNoteLock(selectedNote!!)
                                    }, onPinRequired = {
                                        showNotePinDialog = true
                                    })
                                }
                            } else {
                                if (notePin == null) {
                                    exitSearchMode()
                                    onSettingsClick()
                                } else {
                                    viewModel.toggleNoteLock(selectedNote!!)
                                }
                            }
                            showMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedNote!!.isLocked) "Unlock Note" else "Lock Note")
                    }

                    if (selectedNote!!.isLocked) {
                        TextButton(
                            onClick = {
                                isUnlockingForRename = true
                                isUnlockingForToggle = false
                                authenticateNote(context, onAuthenticated = {
                                    showRenameDialog = true
                                }, onPinRequired = {
                                    showNotePinDialog = true
                                })
                                showMenu = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Rename Locked Note")
                        }
                    }
                }
            }
        )
    }

    if (showRenameDialog && selectedNote != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Locked Note") },
            text = {
                OutlinedTextField(
                    value = newLockedName,
                    onValueChange = { newLockedName = it },
                    label = { Text("New Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        viewModel.insert(selectedNote!!.copy(lockedName = newLockedName))
                        newLockedName = ""
                        showRenameDialog = false
                    }
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNotePinDialog) {
        AlertDialog(
            onDismissRequest = {
                showNotePinDialog = false
                notePinInput = ""
                notePinError = null
            },
            title = { Text("Enter 6-digit Note PIN") },
            text = {
                Column {
                    OutlinedTextField(
                        value = notePinInput,
                        onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) notePinInput = it },
                        label = { Text("PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        isError = notePinError != null
                    )
                    if (notePinError != null) Text(notePinError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (notePinInput == notePin) {
                        showNotePinDialog = false
                        if (isUnlockingForToggle) {
                            viewModel.toggleNoteLock(selectedNote!!)
                        } else if (isUnlockingForRename) {
                            showRenameDialog = true
                        } else {
                            onNoteClick(selectedNote!!.id, searchQuery)
                        }
                        notePinInput = ""
                        notePinError = null
                    } else {
                        notePinError = "Incorrect PIN"
                    }
                }) { Text("Unlock") }
            }
        )
    }
}

private fun authenticateNote(
    context: Context,
    onAuthenticated: () -> Unit,
    onPinRequired: () -> Unit
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
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onPinRequired()
            }
        }
    )
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Note")
        .setSubtitle("Use biometrics to access this note")
        .setNegativeButtonText("Use PIN")
        .build()
    biometricPrompt.authenticate(promptInfo)
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