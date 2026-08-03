package com.enurse.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enurse.app.data.local.datastore.SavedLogin
import com.enurse.app.data.remote.dto.response.UserResponse
import com.enurse.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val userRole: String? = null,
    val codeSent: Boolean = false,
    val profile: UserResponse? = null,
    val profileSaved: Boolean = false,
    val passwordChanged: Boolean = false,
    val feedbackSubmitting: Boolean = false,
    val feedbackSent: Boolean = false,
    val savedLogins: List<SavedLogin> = emptyList(),
    val rememberMe: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    init {
        // Login history survives logout — keep collecting the flows
        viewModelScope.launch {
            authRepository.savedLogins.collect { list ->
                _state.update { it.copy(savedLogins = list) }
            }
        }
        viewModelScope.launch {
            authRepository.rememberMe.collect { remember ->
                _state.update { it.copy(rememberMe = remember) }
            }
        }
    }

    fun login(login: String, password: String, rememberMe: Boolean) {
        viewModelScope.launch {
            // copy() not whole-object rebuild: savedLogins/rememberMe must survive
            _state.update { it.copy(isLoading = true, error = null, success = false) }
            authRepository.login(login, password)
                .onSuccess { res ->
                    authRepository.saveLoginHistory(login, if (rememberMe) password else null, rememberMe)
                    _state.update { it.copy(isLoading = false, success = true, userRole = res.user.role) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "登录失败") }
                }
        }
    }

    fun removeSavedLogin(login: String) {
        viewModelScope.launch { authRepository.removeLoginHistory(login) }
    }

    fun sendCode(email: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)
            authRepository.sendCode(email)
                .onSuccess { msg ->
                    _state.value = _state.value.copy(codeSent = true, error = null)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(error = e.message ?: "发送验证码失败")
                }
        }
    }

    fun register(username: String, displayName: String, email: String, password: String, role: String, code: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, success = false) }
            authRepository.register(username, displayName, email, password, role, code)
                .onSuccess { res ->
                    _state.update { it.copy(isLoading = false, success = true, userRole = res.user.role) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "注册失败") }
                }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            authRepository.deleteAccount()
                .onSuccess { _state.update { it.copy(isLoading = false, success = true, userRole = null) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "注销失败") } }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            authRepository.getProfile()
                .onSuccess { _state.value = _state.value.copy(profile = it, error = null) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message ?: "加载个人信息失败") }
        }
    }

    fun updateProfile(displayName: String) {
        viewModelScope.launch {
            authRepository.updateDisplayName(displayName)
                .onSuccess { _state.value = _state.value.copy(profile = it, profileSaved = true, error = null) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message ?: "修改失败") }
        }
    }

    fun clearProfileSaved() {
        _state.value = _state.value.copy(profileSaved = false)
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, passwordChanged = false, error = null)
            authRepository.changePassword(oldPassword, newPassword)
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false, passwordChanged = true, error = null)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, passwordChanged = false, error = e.message ?: "修改密码失败")
                }
        }
    }

    fun submitFeedback(content: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(feedbackSubmitting = true, feedbackSent = false, error = null)
            authRepository.submitFeedback(content)
                .onSuccess {
                    _state.value = _state.value.copy(feedbackSubmitting = false, feedbackSent = true, error = null)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(feedbackSubmitting = false, feedbackSent = false, error = e.message ?: "反馈提交失败")
                }
        }
    }

    fun clearFeedbackSent() {
        _state.value = _state.value.copy(feedbackSent = false)
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
