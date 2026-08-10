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
import kotlinx.coroutines.isActive
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
        // Poll alerts every 20s
        viewModelScope.launch {
            while (isActive) {
                alertRepository.getAlerts("active")
                    .onSuccess { _state.value = _state.value.copy(alerts = it) }
                kotlinx.coroutines.delay(20_000)
            }
        }
        // Periodic offline sync
        viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(60_000)
                try { bmRepository.syncPending() } catch (_: Exception) {}
            }
        }
    }

    fun recordBm(notes: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRecording = true)
            val userId = tokenDataStore.userId.first()
            if (userId == null) {
                _state.value = _state.value.copy(isRecording = false, error = "无法获取用户信息，请重新登录")
                return@launch
            }
            // 带 +08:00 偏移发送，服务端 new Date() 解析正确（否则被按 UTC 解析，记录偏 8 小时）
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US)
                .format(java.util.Date())

            bmRepository.record(userId, now, notes)
                .onSuccess {
                    _state.value = _state.value.copy(isRecording = false, recordSuccess = true)
                }
                .onFailure { e ->
                    val isNetworkError = e is java.net.ConnectException ||
                        e is java.net.UnknownHostException ||
                        e is java.net.SocketTimeoutException ||
                        e is java.net.SocketException
                    val isAuthError = e is retrofit2.HttpException && (e as retrofit2.HttpException).code() == 401
                    if (isNetworkError || isAuthError) {
                        bmRepository.recordOffline(userId, now, notes)
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
                .onFailure { _state.value = _state.value.copy(error = "解除提醒失败: ${it.message}") }
                .onSuccess { _state.value = _state.value.copy(alerts = _state.value.alerts.filter { it.id != alertId }) }
        }
    }

    fun clearRecordSuccess() {
        _state.value = _state.value.copy(recordSuccess = false, error = null)
    }
}
