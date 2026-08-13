package com.example.securekeep.ui.navigation

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.securekeep.ui.screens.AddEditNoteScreen
import com.example.securekeep.ui.screens.DeletedNotesScreen
import com.example.securekeep.ui.screens.NotesScreen
import com.example.securekeep.ui.screens.SettingsScreen
import com.example.securekeep.viewmodel.DriveAuthViewModel
import com.example.securekeep.viewmodel.DriveSyncViewModel
import com.example.securekeep.viewmodel.NotesViewModel
import com.example.securekeep.ui.components.PinInputField
import com.example.securekeep.ui.components.PinVerificationState

sealed class Screen(val route: String) {
    object Notes : Screen("notes")
    object AddEditNote : Screen("add_edit_note")
    object Settings : Screen("settings")
    object DeletedNotes : Screen("deleted_notes")
}

@Composable
fun NavGraph(
    viewModel: NotesViewModel,
    driveAuthViewModel: DriveAuthViewModel,
    driveSyncViewModel: DriveSyncViewModel
) {
    val navController = rememberNavController()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val appPin by viewModel.appPin.collectAsState()
    val useBiometricApp by viewModel.useBiometricApp.collectAsState()
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val biometricExecutor = remember(context) {
        ContextCompat.getMainExecutor(context)
    }
    var biometricSuccessAction by remember {
        mutableStateOf<(() -> Unit)?>(null)
    }

    var biometricPinRequiredAction by remember {
        mutableStateOf<(() -> Unit)?>(null)
    }

    val biometricPrompt = remember(activity, biometricExecutor) {

        BiometricPrompt(
            activity,
            biometricExecutor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)

                    biometricSuccessAction?.invoke()

                    biometricSuccessAction = null
                    biometricPinRequiredAction = null
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(
                        errorCode,
                        errString
                    )

                    biometricPinRequiredAction?.invoke()

                    biometricSuccessAction = null
                    biometricPinRequiredAction = null
                }
            }
        )
    }

    fun launchBiometricAuthentication(
        onAuthenticated: () -> Unit,
        onPinRequired: () -> Unit
    ) {
        biometricSuccessAction = onAuthenticated
        biometricPinRequiredAction = onPinRequired

        biometricPrompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Secure Keep")
                .setSubtitle("Use biometrics to access your notes")
                .setNegativeButtonText("Use PIN")
                .build()
        )
    }

    var appPinInput by remember { mutableStateOf("") }
    var appPinError by remember { mutableStateOf<String?>(null) }
    var appPinVerificationState by remember {
        mutableStateOf(PinVerificationState.NORMAL)
    }

    var biometricPromptActive by remember {
        mutableStateOf(false)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(
        lifecycleOwner,
        isAppUnlocked,
        useBiometricApp,
        appPin
    ) {
        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_STOP) {

                /*
                 * Do not lock the app because Android is temporarily
                 * displaying our biometric prompt.
                 */
                if (!biometricPromptActive && isAppUnlocked) {

                    viewModel.setAppUnlocked(false)

                    appPinInput = ""
                    appPinError = null
                    appPinVerificationState =
                        PinVerificationState.NORMAL
                }
            }

            if (event == Lifecycle.Event.ON_RESUME) {

                if (appPin != "LOADING_PIN") {

                    if (appPin == null) {

                        viewModel.setAppUnlocked(true)

                    } else if (
                        !isAppUnlocked &&
                        !biometricPromptActive
                    ) {

                        if (useBiometricApp) {

                            biometricPromptActive = true

                            launchBiometricAuthentication(
                                onAuthenticated = {

                                    biometricPromptActive = false
                                    viewModel.setAppUnlocked(true)
                                },
                                onPinRequired = {

                                    biometricPromptActive = false
                                    // PIN dialog remains visible.
                                }
                            )

                        } else {

                            /*
                             * Biometrics are disabled.
                             *
                             * Do not launch anything.
                             * The PIN dialog is already visible.
                             */
                        }
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            appPin == "LOADING_PIN" -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            !isAppUnlocked && appPin != null -> {
                AlertDialog(
                    onDismissRequest = { },
                    title = {
                        Text("App Locked")
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            PinInputField(
                                value = appPinInput,
                                onValueChange = {
                                    appPinInput = it
                                    appPinError = null

                                    if (
                                        appPinVerificationState ==
                                        PinVerificationState.ERROR
                                    ) {
                                        appPinVerificationState =
                                            PinVerificationState.NORMAL
                                    }
                                },
                                verificationState = appPinVerificationState,
                                onComplete = {

                                    if (appPinInput == appPin) {

                                        appPinVerificationState =
                                            PinVerificationState.SUCCESS

                                    } else {

                                        appPinError = "Incorrect PIN"

                                        appPinVerificationState =
                                            PinVerificationState.ERROR
                                    }
                                },
                                onSuccessAnimationFinished = {
                                    viewModel.setAppUnlocked(true)

                                    appPinInput = ""
                                    appPinError = null
                                    appPinVerificationState =
                                        PinVerificationState.NORMAL
                                },
                                onErrorAnimationFinished = {
                                    appPinInput = ""
                                    appPinError = null
                                    appPinVerificationState =
                                        PinVerificationState.NORMAL
                                }
                            )

                            if (appPinError != null) {
                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                Text(
                                    text = appPinError!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            if (useBiometricApp) {
                                Spacer(
                                    modifier = Modifier.height(16.dp)
                                )

                                TextButton(
                                    onClick = {

                                        if (!biometricPromptActive) {

                                            biometricPromptActive = true

                                            launchBiometricAuthentication(
                                                onAuthenticated = {
                                                    biometricPromptActive = false
                                                    viewModel.setAppUnlocked(true)
                                                },
                                                onPinRequired = {
                                                    biometricPromptActive = false
                                                    // PIN dialog remains visible.
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = null
                                    )

                                    Spacer(
                                        modifier = Modifier.width(8.dp)
                                    )

                                    Text("Unlock with Biometrics")
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {}
                )
            }
            else -> {
                // ONCE UNLOCKED, ALL NAVIGATION IS SEAMLESS
                NavHost(
                    navController = navController,
                    startDestination = Screen.Notes.route
                ) {
                    composable(Screen.Notes.route) {
                        NotesScreen(
                            viewModel = viewModel,
                            onAddClick = {
                                navController.navigate(
                                    Screen.AddEditNote.route + "/-1"
                                )
                            },
                            onNoteClick = { noteId, query ->
                                navController.navigate(
                                    Screen.AddEditNote.route + "/$noteId" +
                                            if (!query.isNullOrBlank())
                                                "?searchString=${android.net.Uri.encode(query)}"
                                            else ""
                                )
                            },
                            onSettingsClick = {
                                navController.navigate(
                                    Screen.Settings.route
                                )
                            },
                            onDeletedNotesClick = {
                                navController.navigate(
                                    Screen.DeletedNotes.route
                                )
                            }
                        )
                    }
                    composable(
                        route = Screen.AddEditNote.route + "/{noteId}?searchString={searchString}",
                        arguments = listOf(
                            navArgument("noteId") { type = NavType.IntType; defaultValue = -1 },
                            navArgument("searchString") { type = NavType.StringType; nullable = true; defaultValue = null }
                        )
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getInt("noteId")
                        val query = backStackEntry.arguments?.getString("searchString")
                        AddEditNoteScreen(
                            viewModel = viewModel,
                            noteId = noteId,
                            searchQuery = query,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = viewModel,
                            driveAuthViewModel = driveAuthViewModel,
                            driveSyncViewModel = driveSyncViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.DeletedNotes.route) {
                        DeletedNotesScreen(
                            viewModel = viewModel,
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}