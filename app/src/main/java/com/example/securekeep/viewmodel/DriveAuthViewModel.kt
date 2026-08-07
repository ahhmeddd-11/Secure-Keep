package com.example.securekeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securekeep.auth.GoogleSignInManager
import com.example.securekeep.auth.GoogleUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class DriveAuthViewModel(
    private val signInManager: GoogleSignInManager
) : ViewModel() {
    private val _currentUser = MutableStateFlow<GoogleUser?>(null)
    val currentUser: StateFlow<GoogleUser?> = _currentUser.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        restoreUser()
    }

    val isSignedIn: Boolean
        get() = _currentUser.value != null

    fun signIn() {

        Log.d("DriveAuth", "Sign in button pressed")

        viewModelScope.launch {

            Log.d("DriveAuth", "Coroutine started")

            _loading.value = true
            _error.value = null

            val result = signInManager.signIn()

            Log.d("DriveAuth", "Returned from GoogleSignInManager")

            result.onSuccess {

                Log.d("DriveAuth", "SUCCESS: ${it.user.email}")

                _currentUser.value = it.user

            }.onFailure {

                Log.e("DriveAuth", "FAILED", it)

                _error.value = it.message

            }

            _loading.value = false
        }
    }

    private fun restoreUser() {

        val user = signInManager.restoreUser()

        if (user != null) {
            Log.d("DriveAuth", "Restored previous Google session: ${user.email}")
            _currentUser.value = user
        } else {
            Log.d("DriveAuth", "No previous Google session found")
        }
    }

    fun signOut() {

        viewModelScope.launch {

            signInManager.signOut()

            _currentUser.value = null
        }
    }
}