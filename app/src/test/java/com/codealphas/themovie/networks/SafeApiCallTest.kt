package com.codealphas.themovie.networks

import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.RemoteError
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
    fun `호출이 값을 돌려주면 그 값을 Success로 반환해야 한다`() =
        runTest {
            assertEquals(DataResult.Success(1), safeApiCall { 1 })
        }

    @Test
    fun `소켓 시간이 초과되면 Timeout 실패를 반환해야 한다`() =
        runTest {
            assertFailure(RemoteError.Timeout) { throw SocketTimeoutException() }
        }

    @Test
    fun `그 외 입출력 오류가 나면 Network 실패를 반환해야 한다`() =
        runTest {
            assertFailure(RemoteError.Network) { throw IOException() }
        }

    @Test
    fun `HTTP 오류가 나면 상태 코드를 담은 Http 실패를 반환해야 한다`() =
        runTest {
            val response = Response.error<Unit>(404, "".toResponseBody())

            assertFailure(RemoteError.Http(404)) { throw HttpException(response) }
        }

    @Test
    fun `응답을 기대한 타입으로 읽지 못하면 Serialization 실패를 반환해야 한다`() =
        runTest {
            assertFailure(RemoteError.Serialization) { Json.decodeFromString<Int>("\"not a number\"") }
        }

    @Test
    fun `그 외 예외가 나면 Unknown 실패를 반환해야 한다`() =
        runTest {
            assertFailure(RemoteError.Unknown) { throw IllegalStateException() }
        }

    @Test
    fun `취소 예외가 나면 실패로 바꾸지 않고 다시 던져야 한다`() =
        runTest {
            try {
                safeApiCall { throw CancellationException() }
                fail("CancellationException이 Failure로 바뀜")
            } catch (_: CancellationException) {
            }
        }

    @Test
    fun `응답을 기다리는 중에 호출한 코루틴이 취소되면 safeApiCall 다음 코드가 실행되지 않아야 한다`() =
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
