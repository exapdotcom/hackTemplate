package com.hustle.feature.auth.data

import com.hustle.core.network.ApiResult
import com.hustle.core.network.safeApiCall
import com.hustle.core.storage.TokenStore
import com.hustle.feature.auth.domain.AuthRepository
import com.hustle.feature.auth.domain.SessionState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
) : AuthRepository {

    private val _session = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val session: StateFlow<SessionState> = _session.asStateFlow()

    override suspend fun restore() {
        tokenStore.restore()
        _session.value = if (tokenStore.currentToken != null) {
            SessionState.Authenticated
        } else {
            SessionState.Unauthenticated
        }
    }

    override suspend fun login(email: String, password: String): ApiResult<Unit> =
        handleAuthResponse {
            safeApiCall { api.login(LoginRequest(email = email, password = password)) }
        }

    override suspend fun register(name: String, email: String, password: String): ApiResult<Unit> =
        handleAuthResponse {
            safeApiCall {
                api.register(
                    RegisterRequest(name = name, email = email, password = password),
                )
            }
        }

    override suspend fun logout() {
        tokenStore.clear()
        _session.value = SessionState.Unauthenticated
    }

    private suspend fun handleAuthResponse(
        block: suspend () -> ApiResult<AuthResponse>,
    ): ApiResult<Unit> = when (val result = block()) {
        is ApiResult.Success -> {
            tokenStore.save(result.data.toTokens())
            _session.value = SessionState.Authenticated
            ApiResult.Success(Unit)
        }

        is ApiResult.Failure -> result
    }
}
