package com.enurse.app.domain.usecase.bm

import com.enurse.app.data.remote.dto.response.BmCalendarResponse
import com.enurse.app.domain.repository.BmRepository
import javax.inject.Inject

class GetBmCalendarUseCase @Inject constructor(private val repo: BmRepository) {
    suspend operator fun invoke(year: Int, month: Int): Result<BmCalendarResponse> =
        repo.getCalendar(year, month)
}
