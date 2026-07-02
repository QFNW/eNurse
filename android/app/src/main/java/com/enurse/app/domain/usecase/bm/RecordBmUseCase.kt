package com.enurse.app.domain.usecase.bm

import com.enurse.app.data.remote.dto.response.BmRecordResponse
import com.enurse.app.domain.repository.BmRepository
import javax.inject.Inject

class RecordBmUseCase @Inject constructor(private val repo: BmRepository) {
    suspend operator fun invoke(userId: String, recordedAt: String, notes: String? = null): Result<BmRecordResponse> =
        repo.record(userId, recordedAt, notes)
}
