package com.hustle.core.network

import com.hustle.core.storage.TokenStore
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenStore.currentToken

        if (token.isNullOrBlank() || request.header(HEADER_AUTHORIZATION) != null) {
            return chain.proceed(request)
        }

        return chain.proceed(
            request.newBuilder()
                .header(HEADER_AUTHORIZATION, "Bearer $token")
                .build(),
        )
    }

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
    }
}
