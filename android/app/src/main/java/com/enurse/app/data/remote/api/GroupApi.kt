package com.enurse.app.data.remote.api

import com.enurse.app.data.remote.dto.response.GroupResponse
import com.enurse.app.data.remote.dto.response.GroupMemberResponse
import retrofit2.http.*

interface GroupApi {
    @POST("groups")
    suspend fun create(@Body body: Map<String, String>): GroupResponse

    @GET("groups")
    suspend fun getMyGroups(): List<GroupResponse>

    @GET("groups/{id}")
    suspend fun getById(@Path("id") id: String): GroupResponse

    @POST("groups/join")
    suspend fun join(@Body body: Map<String, String>): GroupMemberResponse

    @GET("groups/{id}/members")
    suspend fun getMembers(@Path("id") id: String): List<GroupMemberResponse>

    @DELETE("groups/{id}/members/{userId}")
    suspend fun removeMember(@Path("id") id: String, @Path("userId") userId: String)

    @GET("groups/{id}/members/{userId}/events")
    suspend fun getMemberEvents(
        @Path("id") id: String,
        @Path("userId") userId: String
    ): Map<String, List<Any>>

    @GET("groups/{id}/members/{userId}/strategies")
    suspend fun getMemberStrategies(
        @Path("id") id: String,
        @Path("userId") userId: String
    ): Map<String, Any>

    @PUT("groups/{id}/members/{userId}/strategies/lock")
    suspend fun toggleLock(
        @Path("id") id: String,
        @Path("userId") userId: String,
        @Body body: Map<String, String>
    ): Any
}
