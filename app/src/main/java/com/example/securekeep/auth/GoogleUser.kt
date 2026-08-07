package com.example.securekeep.auth

data class GoogleUser(
    val id: String,
    val name: String,
    val email: String,
    val profilePicture: String?
)