package com.enurse.app.domain.repository

import com.enurse.app.data.remote.dto.response.AlertResponse

interface AlertRepository {
    suspend fun getAlerts(filter: String = "active"): Result<List<AlertResponse>>
    suspend fun resolve(alertId: String): Result<AlertResponse>
}
