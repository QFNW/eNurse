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
}
