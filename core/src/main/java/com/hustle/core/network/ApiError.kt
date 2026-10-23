package com.hustle.core.network

sealed interface ApiError {
    data class Http(
        val code: Int,
        val message: String? = null,
    ) : ApiError

    data object Network : ApiError

    data class Serialization(
        val message: String? = null,
    ) : ApiError

    data class Unknown(
        val message: String? = null,
    ) : ApiError
}

val ApiError.isUnauthorized: Boolean
    get() = this is ApiError.Http && code == 401
