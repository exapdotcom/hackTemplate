package com.hustle.feature.auth.domain

sealed interface SessionState {
    data object Unknown : SessionState
    data object Authenticated : SessionState
    data object Unauthenticated : SessionState
}
