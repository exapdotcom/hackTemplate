package com.hustle.feature.auth.presentation

import com.hustle.core.presentation.UiEffect
import com.hustle.core.presentation.UiEvent
import com.hustle.core.presentation.UiState
import com.hustle.core.presentation.UiText

enum class AuthMode {
    LOGIN,
    REGISTER,
}

data class AuthState(
    val mode: AuthMode = AuthMode.LOGIN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiText? = null,
) : UiState

sealed interface AuthEvent : UiEvent {
    data class NameChanged(val value: String) : AuthEvent
    data class EmailChanged(val value: String) : AuthEvent
    data class PasswordChanged(val value: String) : AuthEvent
    data class ModeChanged(val mode: AuthMode) : AuthEvent
    data object TogglePasswordVisibility : AuthEvent
    data object Submit : AuthEvent
}

sealed interface AuthEffect : UiEffect {
    data object Authenticated : AuthEffect
}
