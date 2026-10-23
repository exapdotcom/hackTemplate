package com.hustle.core.presentation

import com.hustle.core.R
import com.hustle.core.network.ApiError

fun ApiError.toUiText(): UiText = when (this) {
    is ApiError.Http -> when {
        code == 401 -> UiText.Resource(R.string.core_error_unauthorized)
        code in 500..599 -> UiText.Resource(R.string.core_error_server)
        else -> UiText.Resource(R.string.core_error_http)
    }

    ApiError.Network -> UiText.Resource(R.string.core_error_network)
    is ApiError.Serialization -> UiText.Resource(R.string.core_error_data)
    is ApiError.Unknown -> UiText.Resource(R.string.core_error_unknown)
}
