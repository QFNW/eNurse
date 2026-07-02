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
    val userRole: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            authRepository.login(email, password)
                .onSuccess { res ->
                    _state.value = AuthUiState(success = true, userRole = res.user.role)
                }
                .onFailure { e ->
                    _state.value = AuthUiState(error = e.message ?: "登录失败")
                }
        }
    }

    fun register(email: String, password: String, role: String) {
        viewModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            authRepository.register(email, password, role)
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
