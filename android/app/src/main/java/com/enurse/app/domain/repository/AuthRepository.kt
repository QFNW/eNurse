package com.enurse.app.domain.repository

import com.enurse.app.data.remote.dto.response.AuthResponse

interface AuthRepository {
    suspend fun register(email: String, password: String, role: String): Result<AuthResponse>
    suspend fun login(email: String, password: String): Result<AuthResponse>
    suspend fun refreshToken(refresh: String): Result<AuthResponse>
    suspend fun logout()
}
