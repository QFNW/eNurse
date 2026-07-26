package com.enurse.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enurse.app.data.remote.dto.response.*
import com.enurse.app.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NurseUiState(
    val groups: List<GroupResponse> = emptyList(),
    val selectedGroup: GroupResponse? = null,
    val members: List<GroupMemberResponse> = emptyList(),
    val memberEvents: Map<String, List<Any>>? = null,
    val memberStrategies: Map<String, Any>? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NurseViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NurseUiState())
    val state: StateFlow<NurseUiState> = _state

    private var activeGroupId: String? = null
    private var activeUserId: String? = null

    init {
        // Auto-refresh members + events every 30s
        viewModelScope.launch {
            while (isActive) {
                activeGroupId?.let { gid ->
                    loadGroupDetail(gid)
                    activeUserId?.let { uid -> loadMemberEvents(gid, uid) }
                }
                kotlinx.coroutines.delay(30_000)
            }
        }
    }

    fun loadGroups() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            groupRepository.getMyGroups()
                .onSuccess { _state.value = _state.value.copy(groups = it, isLoading = false) }
                .onFailure { _state.value = _state.value.copy(error = it.message, isLoading = false) }
        }
    }

    fun createGroup(name: String) {
        viewModelScope.launch {
            groupRepository.createGroup(name)
                .onSuccess { loadGroups() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun loadGroupDetail(groupId: String) {
        activeGroupId = groupId
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            groupRepository.getGroupDetail(groupId)
                .onSuccess { _state.value = _state.value.copy(selectedGroup = it, isLoading = false) }
                .onFailure { _state.value = _state.value.copy(error = it.message, isLoading = false) }
        }
    }

    fun loadMembers(groupId: String) {
        viewModelScope.launch {
            groupRepository.getMembers(groupId)
                .onSuccess { _state.value = _state.value.copy(members = it) }
        }
    }

    fun loadMemberEvents(groupId: String, userId: String) {
        activeGroupId = groupId
        activeUserId = userId
        viewModelScope.launch {
            groupRepository.getMemberEvents(groupId, userId)
                .onSuccess { _state.value = _state.value.copy(memberEvents = it) }
        }
    }

    fun loadMemberStrategies(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.getMemberStrategies(groupId, userId)
                .onSuccess { _state.value = _state.value.copy(memberStrategies = it) }
        }
    }

    fun toggleLock(groupId: String, userId: String, target: String, scheduleId: String? = null) {
        viewModelScope.launch {
            groupRepository.toggleLock(groupId, userId, target, scheduleId)
                .onSuccess { loadMemberStrategies(groupId, userId) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}
