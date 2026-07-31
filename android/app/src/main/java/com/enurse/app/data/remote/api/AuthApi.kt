package com.enurse.app.data.remote.api

import com.enurse.app.data.remote.dto.request.LoginRequest
import com.enurse.app.data.remote.dto.request.RegisterRequest
import com.enurse.app.data.remote.dto.response.AuthResponse
import retrofit2.http.*

interface AuthApi {
    @POST("auth/send-code")
    suspend fun sendCode(@Body body: Map<String, String>): Map<String, String>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: Map<String, String>): AuthResponse
}
