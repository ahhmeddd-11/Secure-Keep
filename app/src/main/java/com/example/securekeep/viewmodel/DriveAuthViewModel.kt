package com.example.securekeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securekeep.auth.GoogleSignInManager
import com.example.securekeep.auth.GoogleUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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


        viewModelScope.launch {


            _loading.value = true
            _error.value = null

            val result = signInManager.signIn()


            result.onSuccess {


                _currentUser.value = it.user

            }.onFailure {


                _error.value = it.message

            }

            _loading.value = false
        }
    }

    private fun restoreUser() {

        val user = signInManager.restoreUser()

        if (user != null) {
            _currentUser.value = user
        }
    }

    fun signOut() {

        viewModelScope.launch {

            signInManager.signOut()

            _currentUser.value = null
        }
    }
}