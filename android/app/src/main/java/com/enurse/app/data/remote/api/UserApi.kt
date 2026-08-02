package com.enurse.app.data.remote.api

import com.enurse.app.data.remote.dto.response.UserResponse
import retrofit2.http.*

interface UserApi {
    @GET("users/me")
    suspend fun getProfile(): UserResponse

    @PUT("users/me")
    suspend fun updateProfile(@Body body: Map<String, String>): UserResponse

    @DELETE("users/me")
    suspend fun deleteAccount(): Map<String, String>
}
