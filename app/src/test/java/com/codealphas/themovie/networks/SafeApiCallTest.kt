package com.codealphas.themovie.networks

import com.codealphas.themovie.domain.DataResult
import com.codealphas.themovie.domain.RemoteError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class SafeApiCallTest {
    @Test
    fun returnsSuccessWhenBlockReturns() =
        runTest {
            assertEquals(DataResult.Success(1), safeApiCall { 1 })
        }

    @Test
    fun mapsSocketTimeoutToTimeout() =
        runTest {
            assertFailure(RemoteError.Timeout) { throw SocketTimeoutException() }
        }

    @Test
    fun mapsOtherIoExceptionToNetwork() =
        runTest {
            assertFailure(RemoteError.Network) { throw IOException() }
        }

    @Test
    fun mapsHttpExceptionToHttpWithStatusCode() =
        runTest {
            val response = Response.error<Unit>(404, "".toResponseBody())

            assertFailure(RemoteError.Http(404)) { throw HttpException(response) }
        }

    @Test
    fun mapsDecodingFailureToSerialization() =
        runTest {
            assertFailure(RemoteError.Serialization) { Json.decodeFromString<Int>("\"not a number\"") }
        }

    @Test
    fun mapsOtherExceptionToUnknown() =
        runTest {
            assertFailure(RemoteError.Unknown) { throw IllegalStateException() }
        }

    @Test
    fun rethrowsCancellationException() =
        runTest {
            try {
                safeApiCall { throw CancellationException() }
                fail("CancellationException이 Failure로 바뀜")
            } catch (_: CancellationException) {
            }
        }

    @Test
    fun cancelledCallerDoesNotContinueAfterSafeApiCall() =
        runTest {
            var continued = false
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    safeApiCall { awaitCancellation() }
                    continued = true
                }

            job.cancel()
            job.join()

            assertTrue(job.isCancelled)
            assertFalse(continued)
        }

    private suspend fun assertFailure(
        expected: RemoteError,
        block: suspend () -> Unit,
    ) {
        assertEquals(DataResult.Failure(expected), safeApiCall(block))
    }
}
