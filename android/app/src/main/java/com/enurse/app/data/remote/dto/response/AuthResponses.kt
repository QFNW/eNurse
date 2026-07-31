package com.enurse.app.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    val user: UserBrief
)

data class UserBrief(
    val id: String,
    val email: String,
    val role: String,
    val nickname: String? = null,
    val displayName: String? = null,
    val username: String? = null
)

data class UserResponse(
    val id: String,
    val email: String,
    val role: String,
    val nickname: String?,
    val createdAt: String
)
