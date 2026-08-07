package com.example.securekeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.securekeep.auth.GoogleSignInManager

class DriveAuthViewModelFactory(
    private val signInManager: GoogleSignInManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(DriveAuthViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return DriveAuthViewModel(
                signInManager
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}