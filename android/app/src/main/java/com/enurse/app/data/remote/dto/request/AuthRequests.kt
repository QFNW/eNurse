package com.enurse.app.data.remote.dto.request

data class RegisterRequest(
    val email: String,
    val password: String,
    val role: String
)

data class LoginRequest(
    val email: String,
    val password: String
)
