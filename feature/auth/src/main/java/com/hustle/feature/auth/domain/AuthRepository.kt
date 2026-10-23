package com.hustle.feature.auth.domain

import com.hustle.core.network.ApiResult
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val session: StateFlow<SessionState>

    suspend fun restore()
    suspend fun login(email: String, password: String): ApiResult<Unit>
    suspend fun register(name: String, email: String, password: String): ApiResult<Unit>
    suspend fun logout()
}
