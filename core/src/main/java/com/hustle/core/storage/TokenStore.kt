package com.hustle.core.storage

import kotlinx.coroutines.flow.StateFlow

interface TokenStore {
    val token: StateFlow<String?>
    val currentToken: String?

    suspend fun restore()
    suspend fun save(tokens: AuthTokens)
    suspend fun clear()
}
