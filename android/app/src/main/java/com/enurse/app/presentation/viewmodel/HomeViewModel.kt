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
        // Periodic offline sync
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000) // every 60 seconds
                try { bmRepository.syncPending() } catch (_: Exception) {}
            }
        }
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
            val userId = tokenDataStore.userId.first()
            if (userId == null) {
                _state.value = _state.value.copy(isRecording = false, error = "无法获取用户信息，请重新登录")
                return@launch
            }
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date())

            bmRepository.record(userId, now)
                .onSuccess {
                    _state.value = _state.value.copy(isRecording = false, recordSuccess = true)
                    loadAlerts() // Refresh alerts after recording
                }
                .onFailure { e ->
                    val isNetworkError = e is java.net.ConnectException ||
                        e is java.net.UnknownHostException ||
                        e is java.net.SocketTimeoutException ||
                        e is java.net.SocketException
                    if (isNetworkError) {
                        bmRepository.recordOffline(userId, now)
                        _state.value = _state.value.copy(isRecording = false, recordSuccess = true,
                            error = "网络异常，已本地保存，稍后自动同步")
                    } else {
                        _state.value = _state.value.copy(isRecording = false,
                            error = (e.message ?: "").ifBlank { "打卡失败" })
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
