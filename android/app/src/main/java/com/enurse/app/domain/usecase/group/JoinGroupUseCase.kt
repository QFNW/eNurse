package com.enurse.app.domain.usecase.group

import com.enurse.app.data.remote.dto.response.GroupMemberResponse
import com.enurse.app.domain.repository.GroupRepository
import javax.inject.Inject

class JoinGroupUseCase @Inject constructor(private val repo: GroupRepository) {
    suspend operator fun invoke(inviteCode: String): Result<GroupMemberResponse> =
        repo.joinByInvite(inviteCode)
}
