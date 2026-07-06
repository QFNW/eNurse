package com.enurse.app.data.remote.api

import com.enurse.app.data.remote.dto.response.MedicationResponse
import com.enurse.app.data.remote.dto.response.MedicationRecordResponse
import retrofit2.http.*

interface MedicationApi {
    @POST("medications")
    suspend fun create(@Body body: Map<String, String>): MedicationResponse

    @GET("medications")
    suspend fun getAll(): List<MedicationResponse>

    @PUT("medications/{id}")
    suspend fun update(@Path("id") id: String, @Body body: Map<String, Any>): MedicationResponse

    @DELETE("medications/{id}")
    suspend fun delete(@Path("id") id: String)

    @POST("medications/{id}/schedules")
    suspend fun createSchedule(@Path("id") medId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Any

    @POST("medications/records")
    suspend fun createRecord(@Body body: Map<String, String>): MedicationRecordResponse

    @GET("medications/records")
    suspend fun getRecords(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): List<MedicationRecordResponse>
}
