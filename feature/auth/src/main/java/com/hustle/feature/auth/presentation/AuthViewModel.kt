package com.hustle.feature.auth.presentation

import androidx.lifecycle.viewModelScope
import com.hustle.core.network.ApiResult
import com.hustle.core.presentation.BaseViewModel
import com.hustle.core.presentation.UiText
import com.hustle.core.presentation.toUiText
import com.hustle.feature.auth.R
import com.hustle.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
) : BaseViewModel<AuthState, AuthEvent, AuthEffect>(AuthState()) {

    override fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.NameChanged -> updateState {
                copy(name = event.value, error = null)
            }

            is AuthEvent.EmailChanged -> updateState {
                copy(email = event.value, error = null)
            }

            is AuthEvent.PasswordChanged -> updateState {
                copy(password = event.value, error = null)
            }

            is AuthEvent.ModeChanged -> updateState {
                copy(mode = event.mode, error = null)
            }

            AuthEvent.TogglePasswordVisibility -> updateState {
                copy(isPasswordVisible = !isPasswordVisible)
            }

            AuthEvent.Submit -> submit()
        }
    }

    private fun submit() {
        val state = currentState
        if (state.isLoading) return

        val email = state.email.trim()
        val validationError = when {
            state.mode == AuthMode.REGISTER && state.name.isBlank() ->
                UiText.Resource(R.string.auth_error_name)

            !EMAIL_REGEX.matches(email) ->
                UiText.Resource(R.string.auth_error_email)

            state.password.length < MIN_PASSWORD_LENGTH ->
                UiText.Resource(R.string.auth_error_password)

            else -> null
        }

        if (validationError != null) {
            updateState { copy(error = validationError) }
            return
        }

        updateState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = when (state.mode) {
                AuthMode.LOGIN -> repository.login(email, state.password)
                AuthMode.REGISTER -> repository.register(
                    name = state.name.trim(),
                    email = email,
                    password = state.password,
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    updateState { copy(isLoading = false) }
                    sendEffect(AuthEffect.Authenticated)
                }

                is ApiResult.Failure -> updateState {
                    copy(
                        isLoading = false,
                        error = result.error.toUiText(),
                    )
                }
            }
        }
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
