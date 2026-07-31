package com.enurse.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enurse.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val userRole: String? = null,
    val codeSent: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun login(login: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            authRepository.login(login, password)
                .onSuccess { res ->
                    _state.value = AuthUiState(success = true, userRole = res.user.role)
                }
                .onFailure { e ->
                    _state.value = AuthUiState(error = e.message ?: "登录失败")
                }
        }
    }

    fun sendCode(email: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            authRepository.sendCode(email)
                .onSuccess { msg ->
                    _state.value = _state.value.copy(isLoading = false, codeSent = true, error = null)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message ?: "发送验证码失败")
                }
        }
    }

    fun register(username: String, displayName: String, email: String, password: String, role: String, code: String) {
        viewModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            authRepository.register(username, displayName, email, password, role, code)
                .onSuccess { res ->
                    _state.value = AuthUiState(success = true, userRole = res.user.role)
                }
                .onFailure { e ->
                    _state.value = AuthUiState(error = e.message ?: "注册失败")
                }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
