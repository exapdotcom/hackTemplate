package com.hustle.core.network

import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

/**
 * Converts network exceptions into a small, UI-friendly error model.
 * Never log response bodies or authorization headers here.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (exception: CancellationException) {
    throw exception
} catch (exception: HttpException) {
    val body = exception.response()
        ?.errorBody()
        ?.use { it.string() }
        ?.take(MAX_ERROR_BODY_LENGTH)
    ApiResult.Failure(ApiError.Http(exception.code(), body))
} catch (exception: IOException) {
    ApiResult.Failure(ApiError.Network)
} catch (exception: JsonParseException) {
    ApiResult.Failure(ApiError.Serialization(exception.message))
} catch (exception: Exception) {
    ApiResult.Failure(ApiError.Unknown(exception.message))
}

private const val MAX_ERROR_BODY_LENGTH = 500
