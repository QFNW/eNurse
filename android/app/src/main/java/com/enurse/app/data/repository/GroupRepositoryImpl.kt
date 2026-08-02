package com.enurse.app.data.repository

import com.enurse.app.data.remote.api.GroupApi
import com.enurse.app.data.remote.dto.response.GroupResponse
import com.enurse.app.data.remote.dto.response.GroupMemberResponse
import com.enurse.app.domain.repository.GroupRepository
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val groupApi: GroupApi
) : GroupRepository {

    override suspend fun createGroup(name: String): Result<GroupResponse> =
        runCatching { groupApi.create(mapOf("name" to name)) }

    override suspend fun getMyGroups(): Result<List<GroupResponse>> =
        runCatching { groupApi.getMyGroups() }

    override suspend fun getGroupDetail(id: String): Result<GroupResponse> =
        runCatching { groupApi.getById(id) }

    override suspend fun joinByInvite(inviteCode: String): Result<GroupMemberResponse> =
        runCatching { groupApi.join(mapOf("inviteCode" to inviteCode)) }

    override suspend fun getMembers(groupId: String): Result<List<GroupMemberResponse>> =
        runCatching { groupApi.getMembers(groupId) }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> =
        runCatching { groupApi.removeMember(groupId, userId) }

    override suspend fun leaveGroup(groupId: String): Result<Unit> =
        runCatching { groupApi.leave(groupId) }

    override suspend fun getMemberEvents(groupId: String, userId: String): Result<Map<String, List<Any>>> =
        runCatching { groupApi.getMemberEvents(groupId, userId) }

    override suspend fun getMemberStrategies(groupId: String, userId: String): Result<Map<String, Any>> =
        runCatching { groupApi.getMemberStrategies(groupId, userId) }

    override suspend fun toggleLock(groupId: String, userId: String, target: String, scheduleId: String?): Result<Any> =
        runCatching {
            val body = mutableMapOf("target" to target)
            scheduleId?.let { body["scheduleId"] = it }
            groupApi.toggleLock(groupId, userId, body)
        }

    override suspend fun proxyBm(groupId: String, userId: String, notes: String?): Result<Any> =
        runCatching {
            val b = mutableMapOf<String, String>()
            if (notes != null) b["notes"] = notes
            groupApi.proxyBm(groupId, userId, b)
        }

    override suspend fun proxyMedication(groupId: String, userId: String, medicationId: String, notes: String?): Result<Any> =
        runCatching {
            val b = mutableMapOf("medicationId" to medicationId)
            if (notes != null) b["notes"] = notes
            groupApi.proxyMedication(groupId, userId, b)
        }

    override suspend fun createVirtualPatient(groupId: String, name: String): Result<Any> =
        runCatching { groupApi.createVirtualPatient(groupId, mapOf("name" to name)) }

    override suspend fun createMedicationForPatient(groupId: String, userId: String, name: String, dosage: String?): Result<Any> =
        runCatching {
            val b = mutableMapOf("name" to name)
            if (dosage != null) b["dosage"] = dosage
            groupApi.createMedicationForPatient(groupId, userId, b)
        }

    override suspend fun updateMemberStrategies(groupId: String, userId: String, params: Map<String, String>): Result<Any> =
        runCatching { groupApi.updateMemberStrategies(groupId, userId, params) }

    override suspend fun joinAsStaff(inviteCode: String): Result<Any> =
        runCatching { groupApi.joinAsStaff(mapOf("inviteCode" to inviteCode)) }

    override suspend fun getAdminRequests(groupId: String): Result<List<Any>> =
        runCatching { groupApi.getAdminRequests(groupId) }

    override suspend fun approveAdminRequest(groupId: String, requestId: String): Result<Any> =
        runCatching { groupApi.approveAdminRequest(groupId, requestId) }

    override suspend fun rejectAdminRequest(groupId: String, requestId: String): Result<Any> =
        runCatching { groupApi.rejectAdminRequest(groupId, requestId) }
}
