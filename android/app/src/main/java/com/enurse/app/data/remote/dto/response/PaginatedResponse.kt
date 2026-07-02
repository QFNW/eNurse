package com.enurse.app.data.remote.dto.response

data class BmRecordsResponse(
    val records: List<BmRecordResponse>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)
