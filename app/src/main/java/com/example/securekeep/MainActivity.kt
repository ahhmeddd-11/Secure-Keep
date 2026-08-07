package com.example.securekeep

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.securekeep.auth.GoogleSignInManager
import com.example.securekeep.data.SecurityManager
import com.example.securekeep.data.local.DatabaseProvider
import com.example.securekeep.drive.DriveApiClient
import com.example.securekeep.drive.DriveSyncRepository
import com.example.securekeep.repository.NotesRepository
import com.example.securekeep.sync.worker.BackupScheduler
import com.example.securekeep.ui.navigation.NavGraph
import com.example.securekeep.ui.theme.SecureKeepTheme
import com.example.securekeep.viewmodel.DriveAuthViewModel
import com.example.securekeep.viewmodel.DriveAuthViewModelFactory
import com.example.securekeep.viewmodel.DriveSyncViewModel
import com.example.securekeep.viewmodel.DriveSyncViewModelFactory
import com.example.securekeep.viewmodel.NotesViewModel
import com.example.securekeep.viewmodel.NotesViewModelFactory
import com.google.gson.Gson
import android.os.Handler
import android.os.Looper

class MainActivity : FragmentActivity() {

    private lateinit var viewModel: NotesViewModel
    private lateinit var driveAuthViewModel: DriveAuthViewModel
    private lateinit var driveSyncViewModel: DriveSyncViewModel

    private val lockHandler = Handler(Looper.getMainLooper())

    private val lockRunnable = Runnable {
        if (::viewModel.isInitialized) {
            viewModel.setAppUnlocked(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide preview in Recents
//        window.setFlags(
//            android.view.WindowManager.LayoutParams.FLAG_SECURE,
//            android.view.WindowManager.LayoutParams.FLAG_SECURE
//        )

        enableEdgeToEdge()

        val dao = DatabaseProvider.provideDatabase(this).noteDao()
        val repository = NotesRepository(dao)
        val securityManager = SecurityManager(this)

        val notesFactory = NotesViewModelFactory(repository, securityManager)
        viewModel = ViewModelProvider(this, notesFactory)[NotesViewModel::class.java]

        val signInManager = GoogleSignInManager(this)
        driveAuthViewModel = ViewModelProvider(
            this,
            DriveAuthViewModelFactory(signInManager)
        )[DriveAuthViewModel::class.java]

        // Build Drive sync components
        val driveApiClient = DriveApiClient(this)
        val driveSyncRepository = DriveSyncRepository(this, driveApiClient, Gson())

        driveSyncViewModel = ViewModelProvider(
            this,
            DriveSyncViewModelFactory(driveSyncRepository, repository)
        )[DriveSyncViewModel::class.java]

        // Schedule daily backup (KEEP policy — safe to call on every launch)
        BackupScheduler.scheduleDailyBackup(this)

        setContent {

            val isDarkModePref by viewModel.isDarkMode.collectAsState()
            val darkTheme = isDarkModePref ?: isSystemInDarkTheme()

            val currentUser by driveAuthViewModel.currentUser.collectAsState()

            // Persist account email whenever the signed-in user changes
            androidx.compose.runtime.LaunchedEffect(currentUser) {
                val email = currentUser?.email
                if (email != null) {
                    driveSyncViewModel.onUserSignedIn(email)
                }
            }

            SecureKeepTheme(darkTheme = darkTheme) {
                NavGraph(
                    viewModel = viewModel,
                    driveAuthViewModel = driveAuthViewModel,
                    driveSyncViewModel = driveSyncViewModel
                )
            }
        }

    }
    override fun onStart() {
        super.onStart()

        lockHandler.removeCallbacks(lockRunnable)
    }

    override fun onStop() {
        super.onStop()

        lockHandler.postDelayed(lockRunnable, 1000)
    }

    override fun onResume() {
        super.onResume()
    }
}