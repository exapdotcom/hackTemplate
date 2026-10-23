package com.hustle.core.storage

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
)
