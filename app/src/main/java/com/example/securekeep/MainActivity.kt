package com.example.securekeep

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.securekeep.data.SecurityManager
import com.example.securekeep.data.local.DatabaseProvider
import com.example.securekeep.repository.NotesRepository
import com.example.securekeep.ui.navigation.NavGraph
import com.example.securekeep.ui.theme.SecureKeepTheme
import com.example.securekeep.viewmodel.NotesViewModel
import com.example.securekeep.viewmodel.NotesViewModelFactory

class MainActivity : FragmentActivity() {
    private lateinit var viewModel: NotesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide preview in Recents continuously and perfectly globally
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        
        val dao = DatabaseProvider.provideDatabase(this).noteDao()
        val repository = NotesRepository(dao)
        val securityManager = SecurityManager(this)
        val factory = NotesViewModelFactory(repository, securityManager)
        viewModel = ViewModelProvider(this, factory)[NotesViewModel::class.java]

        setContent {
            val isDarkModePref by viewModel.isDarkMode.collectAsState()
            val darkTheme = isDarkModePref ?: isSystemInDarkTheme()
            
            SecureKeepTheme(darkTheme = darkTheme) {
                NavGraph(viewModel = viewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Lock immediately upon backgrounding, unless it's a configuration change like rotation
        if (::viewModel.isInitialized && !isChangingConfigurations) {
            viewModel.setAppUnlocked(false)
        }
    }

    override fun onResume() {
        super.onResume()
    }
}
