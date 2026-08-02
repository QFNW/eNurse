package com.enurse.app.domain.repository

import com.enurse.app.data.remote.dto.response.GroupResponse
import com.enurse.app.data.remote.dto.response.GroupMemberResponse

interface GroupRepository {
    suspend fun createGroup(name: String): Result<GroupResponse>
    suspend fun getMyGroups(): Result<List<GroupResponse>>
    suspend fun getGroupDetail(id: String): Result<GroupResponse>
    suspend fun joinByInvite(inviteCode: String): Result<GroupMemberResponse>
    suspend fun getMembers(groupId: String): Result<List<GroupMemberResponse>>
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>
    suspend fun leaveGroup(groupId: String): Result<Unit>
    suspend fun getMemberEvents(groupId: String, userId: String): Result<Map<String, List<Any>>>
    suspend fun getMemberStrategies(groupId: String, userId: String): Result<Map<String, Any>>
    suspend fun toggleLock(groupId: String, userId: String, target: String, scheduleId: String? = null): Result<Any>
    suspend fun proxyBm(groupId: String, userId: String, notes: String?): Result<Any>
    suspend fun proxyMedication(groupId: String, userId: String, medicationId: String, notes: String?): Result<Any>
    suspend fun createVirtualPatient(groupId: String, name: String): Result<Any>
    suspend fun createMedicationForPatient(groupId: String, userId: String, name: String, dosage: String?): Result<Any>
    suspend fun updateMemberStrategies(groupId: String, userId: String, params: Map<String, String>): Result<Any>
    suspend fun joinAsStaff(inviteCode: String): Result<Any>
    suspend fun getAdminRequests(groupId: String): Result<List<Any>>
    suspend fun approveAdminRequest(groupId: String, requestId: String): Result<Any>
    suspend fun rejectAdminRequest(groupId: String, requestId: String): Result<Any>
}
