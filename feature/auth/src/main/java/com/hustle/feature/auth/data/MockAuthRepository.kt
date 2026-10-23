package com.hustle.feature.auth.data

import com.hustle.core.config.MockScenario
import com.hustle.core.config.MockScenarioController
import com.hustle.core.network.ApiError
import com.hustle.core.network.ApiResult
import com.hustle.core.storage.AuthTokens
import com.hustle.core.storage.TokenStore
import com.hustle.feature.auth.domain.AuthRepository
import com.hustle.feature.auth.domain.SessionState
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockAuthRepository @Inject constructor(
    private val tokenStore: TokenStore,
    private val scenarioController: MockScenarioController,
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
        completeMockAuth()

    override suspend fun register(name: String, email: String, password: String): ApiResult<Unit> =
        completeMockAuth()

    override suspend fun logout() {
        tokenStore.clear()
        _session.value = SessionState.Unauthenticated
    }

    private suspend fun completeMockAuth(): ApiResult<Unit> {
        when (scenarioController.scenario.value) {
            MockScenario.ERROR -> {
                delay(500)
                return ApiResult.Failure(ApiError.Http(401, "Mock credentials rejected"))
            }

            MockScenario.SLOW -> delay(1_800)
            MockScenario.SUCCESS, MockScenario.EMPTY -> delay(600)
        }

        tokenStore.save(
            AuthTokens(
                accessToken = "mock-access-${System.currentTimeMillis()}",
            ),
        )
        _session.value = SessionState.Authenticated
        return ApiResult.Success(Unit)
    }
}
