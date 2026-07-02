package com.enurse.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enurse.app.data.local.datastore.TokenDataStore
import com.enurse.app.data.remote.dto.response.AlertResponse
import com.enurse.app.domain.repository.AlertRepository
import com.enurse.app.domain.repository.BmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val alerts: List<AlertResponse> = emptyList(),
    val isRecording: Boolean = false,
    val recordSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bmRepository: BmRepository,
    private val alertRepository: AlertRepository,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        viewModelScope.launch {
            alertRepository.getAlerts("active")
                .onSuccess { _state.value = _state.value.copy(alerts = it) }
        }
    }

    fun recordBm() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRecording = true)
            val userId = tokenDataStore.userId.first() ?: return@launch
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date())

            bmRepository.record(userId, now)
                .onSuccess {
                    _state.value = _state.value.copy(isRecording = false, recordSuccess = true)
                    loadAlerts() // Refresh alerts after recording
                }
                .onFailure { e ->
                    // Only fall back to offline on network errors, not validation/auth failures
                    val msg = e.message ?: ""
                    val isNetworkError = msg.contains("timeout") || msg.contains("connect") ||
                        msg.contains("network") || msg.contains("unreachable")
                    if (isNetworkError) {
                        bmRepository.recordOffline(userId, now)
                        _state.value = _state.value.copy(isRecording = false, recordSuccess = true,
                            error = "网络异常，已本地保存，稍后自动同步")
                    } else {
                        _state.value = _state.value.copy(isRecording = false,
                            error = msg.ifBlank { "打卡失败" })
                    }
                }
        }
    }

    fun resolveAlert(alertId: String) {
        viewModelScope.launch {
            alertRepository.resolve(alertId)
            loadAlerts()
        }
    }

    fun clearRecordSuccess() {
        _state.value = _state.value.copy(recordSuccess = false, error = null)
    }
}
