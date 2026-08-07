package com.example.securekeep.auth

data class GoogleSignInResult(
    val user: GoogleUser,
    val idToken: String
)