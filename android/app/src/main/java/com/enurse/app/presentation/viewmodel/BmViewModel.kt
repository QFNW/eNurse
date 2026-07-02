package com.enurse.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enurse.app.data.remote.dto.response.BmCalendarResponse
import com.enurse.app.data.remote.dto.response.BmSettingsResponse
import com.enurse.app.domain.repository.BmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class BmUiState(
    val calendar: BmCalendarResponse? = null,
    val settings: BmSettingsResponse? = null,
    val isLoading: Boolean = false,
    val selectedDay: String? = null,
    val selectedDayRecords: List<com.enurse.app.data.remote.dto.response.BmRecordResponse> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class BmViewModel @Inject constructor(
    private val bmRepository: BmRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BmUiState())
    val state: StateFlow<BmUiState> = _state

    private val calendar = Calendar.getInstance()

    init {
        loadCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        loadSettings()
    }

    fun loadCalendar(year: Int, month: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            bmRepository.getCalendar(year, month)
                .onSuccess { _state.value = _state.value.copy(calendar = it, isLoading = false) }
                .onFailure { _state.value = _state.value.copy(error = it.message, isLoading = false) }
        }
    }

    fun selectDay(date: String) {
        val records = _state.value.calendar?.days
            ?.find { it.date == date }?.records ?: emptyList()
        _state.value = _state.value.copy(selectedDay = date, selectedDayRecords = records)
    }

    fun loadSettings() {
        viewModelScope.launch {
            bmRepository.getSettings()
                .onSuccess { _state.value = _state.value.copy(settings = it) }
        }
    }

    fun updateSettings(intervalDays: Int?, reminderTime: String?, isEnabled: Boolean?) {
        viewModelScope.launch {
            bmRepository.updateSettings(intervalDays, reminderTime, isEnabled)
                .onSuccess { _state.value = _state.value.copy(settings = it) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}
