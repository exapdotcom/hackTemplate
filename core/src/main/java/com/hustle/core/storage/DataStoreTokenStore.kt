package com.hustle.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hustle.core.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.authDataStore by preferencesDataStore(name = "auth_tokens")

/**
 * Simple token store for the template. The interface makes it possible to swap
 * this implementation for an encrypted one without touching features.
 */
@Singleton
class DataStoreTokenStore @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) : TokenStore {

    private val dataStore = context.authDataStore
    private val _token = MutableStateFlow<String?>(null)
    override val token: StateFlow<String?> = _token.asStateFlow()
    override val currentToken: String?
        get() = _token.value

    private val ready = CompletableDeferred<Unit>()

    init {
        scope.launch {
            runCatching { dataStore.data.first() }
                .onSuccess { preferences -> _token.value = preferences[ACCESS_TOKEN] }
            ready.complete(Unit)
        }
    }

    override suspend fun restore() {
        ready.await()
    }

    override suspend fun save(tokens: AuthTokens) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = tokens.accessToken
            if (tokens.refreshToken != null) {
                preferences[REFRESH_TOKEN] = tokens.refreshToken
            } else {
                preferences.remove(REFRESH_TOKEN)
            }
        }
        _token.value = tokens.accessToken
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
        _token.value = null
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }
}
