package com.enurse.app.data.repository

import com.enurse.app.data.local.datastore.TokenDataStore
import com.enurse.app.data.remote.api.AuthApi
import com.enurse.app.data.remote.dto.request.LoginRequest
import com.enurse.app.data.remote.dto.request.RegisterRequest
import com.enurse.app.data.remote.dto.response.AuthResponse
import com.enurse.app.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenDataStore: TokenDataStore
) : AuthRepository {

    override suspend fun register(username: String, displayName: String, email: String, password: String, role: String): Result<AuthResponse> =
        runCatching {
            val res = authApi.register(RegisterRequest(username, displayName, email, password, role))
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

    override suspend fun logout() {
        tokenDataStore.clear()
    }
}
