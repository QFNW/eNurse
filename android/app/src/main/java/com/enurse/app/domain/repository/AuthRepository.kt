package com.enurse.app.domain.repository

import com.enurse.app.data.remote.dto.response.AuthResponse
import com.enurse.app.data.remote.dto.response.UserResponse

interface AuthRepository {
    suspend fun sendCode(email: String): Result<String>
    suspend fun register(username: String, displayName: String, email: String, password: String, role: String, code: String): Result<AuthResponse>
    suspend fun login(login: String, password: String): Result<AuthResponse>
    suspend fun refreshToken(refresh: String): Result<AuthResponse>
    suspend fun getProfile(): Result<UserResponse>
    suspend fun updateDisplayName(displayName: String): Result<UserResponse>
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<String>
    suspend fun submitFeedback(content: String): Result<String>
    suspend fun deleteAccount(): Result<String>
    suspend fun logout()
}
