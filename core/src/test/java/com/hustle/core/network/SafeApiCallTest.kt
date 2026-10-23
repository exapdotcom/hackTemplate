package com.hustle.core.network

import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeApiCallTest {

    @Test
    fun `successful call is wrapped in success`() = runTest {
        val result = safeApiCall { "payload" }

        assertEquals(ApiResult.Success("payload"), result)
    }

    @Test
    fun `io exception becomes network error`() = runTest {
        val result = safeApiCall<String> { throw IOException("offline") }

        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiError.Network, (result as ApiResult.Failure).error)
    }
}
