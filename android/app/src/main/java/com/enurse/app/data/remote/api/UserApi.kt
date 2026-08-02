package com.enurse.app.data.remote.api

import com.enurse.app.data.remote.dto.response.UserResponse
import retrofit2.http.*

interface UserApi {
    @GET("users/me")
    suspend fun getProfile(): UserResponse

    @PUT("users/me")
    suspend fun updateProfile(@Body body: Map<String, String>): UserResponse

    @PUT("users/me/password")
    suspend fun changePassword(@Body body: Map<String, String>): Map<String, String>

    @POST("feedback")
    suspend fun submitFeedback(@Body body: Map<String, String>): Map<String, String>

    @DELETE("users/me")
    suspend fun deleteAccount(): Map<String, String>
}
