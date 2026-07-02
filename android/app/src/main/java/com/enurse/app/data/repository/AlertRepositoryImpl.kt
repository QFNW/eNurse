package com.enurse.app.data.repository

import com.enurse.app.data.remote.api.AlertApi
import com.enurse.app.data.remote.dto.response.AlertResponse
import com.enurse.app.domain.repository.AlertRepository
import javax.inject.Inject

class AlertRepositoryImpl @Inject constructor(
    private val alertApi: AlertApi
) : AlertRepository {

    override suspend fun getAlerts(filter: String): Result<List<AlertResponse>> =
        runCatching { alertApi.getAlerts(filter) }

    override suspend fun resolve(alertId: String): Result<AlertResponse> =
        runCatching { alertApi.resolve(alertId) }
}
