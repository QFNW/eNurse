package com.enurse.app.data.remote.api

import com.enurse.app.data.remote.dto.request.CreateMedicationForPatientRequest
import com.enurse.app.data.remote.dto.request.UpdateMemberStrategiesRequest
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

    @POST("groups/{id}/leave")
    suspend fun leave(@Path("id") id: String): Any

    @DELETE("groups/{id}/members/{userId}")
    suspend fun removeMember(@Path("id") id: String, @Path("userId") userId: String)

    @GET("groups/{id}/members/{userId}/events")
    suspend fun getMemberEvents(
        @Path("id") id: String,
        @Path("userId") userId: String
    ): Map<String, @JvmSuppressWildcards List<@JvmSuppressWildcards Any>>

    @GET("groups/{id}/members/{userId}/strategies")
    suspend fun getMemberStrategies(
        @Path("id") id: String,
        @Path("userId") userId: String
    ): Map<String, @JvmSuppressWildcards Any>

    @PUT("groups/{id}/members/{userId}/strategies/lock")
    suspend fun toggleLock(
        @Path("id") id: String,
        @Path("userId") userId: String,
        @Body body: Map<String, String>
    ): Any

    @POST("groups/{id}/members/{userId}/proxy-bm")
    suspend fun proxyBm(
        @Path("id") id: String,
        @Path("userId") userId: String,
        @Body body: Map<String, String>
    ): Any

    @POST("groups/{id}/members/{userId}/proxy-medication")
    suspend fun proxyMedication(
        @Path("id") id: String,
        @Path("userId") userId: String,
        @Body body: Map<String, String>
    ): Any

    @POST("groups/{id}/virtual-patients")
    suspend fun createVirtualPatient(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Any

    @POST("groups/{id}/members/{userId}/medications")
    suspend fun createMedicationForPatient(
        @Path("id") id: String,
        @Path("userId") userId: String,
        @Body body: CreateMedicationForPatientRequest
    ): Any

    @PUT("groups/{id}/members/{userId}/strategies")
    suspend fun updateMemberStrategies(
        @Path("id") id: String,
        @Path("userId") userId: String,
        @Body body: UpdateMemberStrategiesRequest
    ): Any

    @POST("groups/join-staff")
    suspend fun joinAsStaff(@Body body: Map<String, String>): Any

    @GET("groups/{id}/admin-requests")
    suspend fun getAdminRequests(@Path("id") id: String): List<Any>

    @PUT("groups/{id}/admin-requests/{requestId}/approve")
    suspend fun approveAdminRequest(@Path("id") id: String, @Path("requestId") requestId: String): Any

    @DELETE("groups/{id}/admin-requests/{requestId}")
    suspend fun rejectAdminRequest(@Path("id") id: String, @Path("requestId") requestId: String)
}
