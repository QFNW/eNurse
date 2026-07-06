package com.enurse.app.data.remote.dto.request

data class RegisterRequest(
    val username: String,
    val displayName: String,
    val email: String,
    val password: String,
    val role: String
)

data class LoginRequest(
    val login: String,
    val password: String
)
