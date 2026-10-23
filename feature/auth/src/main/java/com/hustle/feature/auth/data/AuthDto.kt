package com.hustle.feature.auth.data

import com.google.gson.annotations.SerializedName
import com.hustle.core.storage.AuthTokens

data class LoginRequest(
    val email: String,
    val password: String,
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
)

data class AuthResponse(
    @SerializedName(value = "accessToken", alternate = ["access_token", "token"])
    val accessToken: String,
    @SerializedName(value = "refreshToken", alternate = ["refresh_token"])
    val refreshToken: String? = null,
)

fun AuthResponse.toTokens(): AuthTokens = AuthTokens(
    accessToken = accessToken,
    refreshToken = refreshToken,
)
