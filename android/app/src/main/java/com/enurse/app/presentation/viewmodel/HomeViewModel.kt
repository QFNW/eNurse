package com.enurse.app.presentation.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enurse.app.data.local.datastore.TokenDataStore
import com.enurse.app.data.remote.dto.response.AlertResponse
import com.enurse.app.domain.repository.AlertRepository
import com.enurse.app.domain.repository.BmRepository
import com.enurse.app.domain.repository.MedicationRepository
import com.enurse.app.presentation.alarm.AlarmScheduler
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
    app: android.app.Application,
    private val bmRepository: BmRepository,
    private val alertRepository: AlertRepository,
    private val medicationRepository: MedicationRepository,
    private val tokenDataStore: TokenDataStore
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init {
        loadAlerts()
        // Poll alerts every 20s with ring/vibrate for new alerts
        viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(20_000)
                val oldIds = _state.value.alerts.map { it.id }.toSet()
                alertRepository.getAlerts("active")
                    .onSuccess { newAlerts ->
                        _state.value = _state.value.copy(alerts = newAlerts)
                        // Ring for genuinely new alerts
                        val fresh = newAlerts.filter { it.id !in oldIds }
                        if (fresh.isNotEmpty()) {
                            _state.value = _state.value.copy(error = "⏰ 用药提醒：${fresh.first().message}")
                            try {
                                val app = getApplication<android.app.Application>()
                                val vibrator = app.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                vibrator?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 500, 500, 500, 500), -1))
                                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                                android.media.RingtoneManager.getRingtone(app, uri)?.play()
                            } catch (_: Exception) {}
                        }
                    }
                    .onFailure { _state.value = _state.value.copy(error = "提醒检查失败: ${it.message}") }
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

    fun loadAlerts() {
        viewModelScope.launch {
            alertRepository.getAlerts("active")
                .onSuccess { _state.value = _state.value.copy(alerts = it) }
        }
    }

    fun scheduleAlarms(context: android.content.Context) {
        viewModelScope.launch {
            medicationRepository.getMedications()
                .onSuccess { meds ->
                    val schedules = meds.flatMap { med ->
                        (med.schedules ?: emptyList()).map { Pair(med.name, it) }
                    }
                    if (schedules.isNotEmpty()) {
                        AlarmScheduler.cancelAll(context)
                        AlarmScheduler.scheduleMedicationAlarms(context, schedules)
                    }
                }
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
                    val isAuthError = e is retrofit2.HttpException && (e as retrofit2.HttpException).code() == 401
                    if (isNetworkError || isAuthError) {
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
