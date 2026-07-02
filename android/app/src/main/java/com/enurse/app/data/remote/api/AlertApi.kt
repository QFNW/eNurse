package com.enurse.app.data.remote.api

import com.enurse.app.data.remote.dto.response.AlertResponse
import retrofit2.http.*

interface AlertApi {
    @GET("alerts")
    suspend fun getAlerts(@Query("filter") filter: String? = "active"): List<AlertResponse>

    @PUT("alerts/{id}/resolve")
    suspend fun resolve(@Path("id") id: String): AlertResponse
}
