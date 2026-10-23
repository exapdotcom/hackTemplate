package com.hustle.feature.auth

import com.hustle.core.network.ApiError
import com.hustle.core.network.ApiResult
import com.hustle.feature.auth.domain.AuthRepository
import com.hustle.feature.auth.domain.SessionState
import com.hustle.feature.auth.presentation.AuthEffect
import com.hustle.feature.auth.presentation.AuthEvent
import com.hustle.feature.auth.presentation.AuthViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `successful login emits authenticated effect`() = runTest {
        val viewModel = AuthViewModel(
            repository = FakeAuthRepository(ApiResult.Success(Unit)),
        )
        val effect = async { viewModel.effects.first() }

        viewModel.onEvent(AuthEvent.EmailChanged("demo@example.com"))
        viewModel.onEvent(AuthEvent.PasswordChanged("password"))
        viewModel.onEvent(AuthEvent.Submit)
        advanceUntilIdle()

        assertEquals(AuthEffect.Authenticated, effect.await())
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `failed login shows an error`() = runTest {
        val viewModel = AuthViewModel(
            repository = FakeAuthRepository(ApiResult.Failure(ApiError.Http(401))),
        )

        viewModel.onEvent(AuthEvent.EmailChanged("demo@example.com"))
        viewModel.onEvent(AuthEvent.PasswordChanged("password"))
        viewModel.onEvent(AuthEvent.Submit)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    private class FakeAuthRepository(
        private val result: ApiResult<Unit>,
    ) : AuthRepository {
        private val _session = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
        override val session: StateFlow<SessionState> = _session

        override suspend fun restore() = Unit

        override suspend fun login(email: String, password: String): ApiResult<Unit> = result

        override suspend fun register(
            name: String,
            email: String,
            password: String,
        ): ApiResult<Unit> = result

        override suspend fun logout() {
            _session.value = SessionState.Unauthenticated
        }
    }
}
