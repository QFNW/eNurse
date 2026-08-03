package com.enurse.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enurse.app.data.local.datastore.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashUiState {
    data object NotReady : SplashUiState
    data class Ready(val startDest: String) : SplashUiState
}

/**
 * Decides the first destination during splash: reads the locally stored token
 * and role. Token validity is not checked here — the first business request
 * triggers the 401 → refresh chain (see AuthInterceptor).
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _state = MutableStateFlow<SplashUiState>(SplashUiState.NotReady)
    val state: StateFlow<SplashUiState> = _state

    init {
        viewModelScope.launch {
            // runCatching: a DataStore read failure must never leave the system
            // splash on screen forever — fall back to the login screen.
            val token = runCatching { tokenDataStore.accessToken.first() }.getOrNull()
            val role = runCatching { tokenDataStore.userRole.first() }.getOrNull()
            val dest = if (token != null) {
                when (role) { "NURSE" -> "nurse/main" else -> "patient/main" }
            } else {
                "auth/login"
            }
            _state.value = SplashUiState.Ready(dest)
        }
    }
}
