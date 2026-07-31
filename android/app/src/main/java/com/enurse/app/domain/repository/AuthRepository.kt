package com.enurse.app.domain.repository

import com.enurse.app.data.remote.dto.response.AuthResponse

interface AuthRepository {
    suspend fun sendCode(email: String): Result<String>
    suspend fun register(username: String, displayName: String, email: String, password: String, role: String, code: String): Result<AuthResponse>
    suspend fun login(login: String, password: String): Result<AuthResponse>
    suspend fun refreshToken(refresh: String): Result<AuthResponse>
    suspend fun logout()
}
