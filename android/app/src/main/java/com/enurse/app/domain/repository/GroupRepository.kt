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
    suspend fun getMemberEvents(groupId: String, userId: String): Result<Map<String, List<Any>>>
    suspend fun getMemberStrategies(groupId: String, userId: String): Result<Map<String, Any>>
    suspend fun toggleLock(groupId: String, userId: String, target: String, scheduleId: String? = null): Result<Any>
}
