package com.enurse.app.data.repository

import com.enurse.app.data.local.datastore.TokenDataStore
import com.enurse.app.data.remote.api.AuthApi
import com.enurse.app.data.remote.api.UserApi
import com.enurse.app.data.remote.dto.request.LoginRequest
import com.enurse.app.data.remote.dto.request.RegisterRequest
import com.enurse.app.data.remote.dto.response.AuthResponse
import com.enurse.app.data.remote.dto.response.UserResponse
import com.enurse.app.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userApi: UserApi,
    private val tokenDataStore: TokenDataStore
) : AuthRepository {

    override suspend fun sendCode(email: String): Result<String> =
        runCatching {
            val res = authApi.sendCode(mapOf("email" to email))
            res["message"] ?: "验证码已发送"
        }

    override suspend fun register(username: String, displayName: String, email: String, password: String, role: String, code: String): Result<AuthResponse> =
        runCatching {
            val res = authApi.register(RegisterRequest(username, displayName, email, password, role, code))
            tokenDataStore.saveTokens(res.accessToken, res.refreshToken)
            tokenDataStore.saveUserInfo(res.user.id, res.user.role)
            res
        }

    override suspend fun login(login: String, password: String): Result<AuthResponse> =
        runCatching {
            val res = authApi.login(LoginRequest(login, password))
            tokenDataStore.saveTokens(res.accessToken, res.refreshToken)
            tokenDataStore.saveUserInfo(res.user.id, res.user.role)
            res
        }

    override suspend fun refreshToken(refresh: String): Result<AuthResponse> =
        runCatching {
            val res = authApi.refresh(mapOf("refreshToken" to refresh))
            tokenDataStore.saveTokens(res.accessToken, res.refreshToken)
            res
        }

    override suspend fun getProfile(): Result<UserResponse> =
        runCatching { userApi.getProfile() }

    override suspend fun updateDisplayName(displayName: String): Result<UserResponse> =
        runCatching { userApi.updateProfile(mapOf("displayName" to displayName)) }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<String> =
        runCatching {
            val res = userApi.changePassword(mapOf("oldPassword" to oldPassword, "newPassword" to newPassword))
            res["message"] ?: "密码已修改"
        }

    override suspend fun submitFeedback(content: String): Result<String> =
        runCatching {
            val res = userApi.submitFeedback(mapOf("content" to content))
            res["message"] ?: "反馈已提交"
        }

    override suspend fun deleteAccount(): Result<String> =
        runCatching {
            val res = userApi.deleteAccount()
            tokenDataStore.clear()
            res["message"] ?: "账号已注销"
        }

    override suspend fun logout() {
        tokenDataStore.clear()
    }
}
