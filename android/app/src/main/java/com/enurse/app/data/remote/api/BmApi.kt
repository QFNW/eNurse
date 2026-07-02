package com.enurse.app.data.remote.api

import com.enurse.app.data.remote.dto.response.BmCalendarResponse
import com.enurse.app.data.remote.dto.response.BmRecordResponse
import com.enurse.app.data.remote.dto.response.BmSettingsResponse
import retrofit2.http.*

interface BmApi {
    @POST("bm/records")
    suspend fun createRecord(@Body body: Map<String, String>): BmRecordResponse

    @GET("bm/records")
    suspend fun getRecords(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): List<BmRecordResponse>

    @DELETE("bm/records/{id}")
    suspend fun deleteRecord(@Path("id") id: String)

    @GET("bm/calendar")
    suspend fun getCalendar(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): BmCalendarResponse

    @GET("bm/settings")
    suspend fun getSettings(): BmSettingsResponse

    @PUT("bm/settings")
    suspend fun updateSettings(@Body body: Map<String, Any>): BmSettingsResponse
}
