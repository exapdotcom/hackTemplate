package com.hustle.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hustle.core.config.AppConfig
import com.hustle.feature.auth.domain.AuthRepository
import com.hustle.feature.auth.domain.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AppSessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    val config: AppConfig,
) : ViewModel() {

    val session: StateFlow<SessionState> = authRepository.session

    init {
        viewModelScope.launch {
            authRepository.restore()
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
