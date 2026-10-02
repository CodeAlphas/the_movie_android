package com.codealphas.themovie.data.auth

import com.codealphas.themovie.domain.auth.AuthError
import com.codealphas.themovie.domain.auth.AuthResult
import com.codealphas.themovie.domain.result.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

// Firebase 예외 생성자가 부르는 TextUtils.isEmpty는 JVM 단위 테스트의 android.jar에서 실제 동작 대신 not mocked 예외를 던지므로,
// Firebase 예외 종류별 AuthError 매핑 검증은 이 테스트에서 제외
@OptIn(ExperimentalCoroutinesApi::class)
class AuthErrorMapperTest {
    @Test
    fun `블록이 끝나면 Success를 반환해야 한다`() =
        runTest {
            assertEquals(Outcome.Success(Unit), toAuthResult { "uid" })
        }

    @Test
    fun `호출한 코루틴이 취소되지 않았는데 블록이 취소 예외를 던지면 Unknown 실패를 반환해야 한다`() =
        runTest {
            assertEquals(Outcome.Failure(AuthError.Unknown), toAuthResult { throw CancellationException("SDK 취소") })
        }

    @Test
    fun `블록 실행 중에 호출한 코루틴이 취소되면 실패를 반환하지 않고 취소되어야 한다`() =
        runTest {
            var result: AuthResult? = null
            val job = launch { result = toAuthResult { awaitCancellation() } }
            runCurrent()

            job.cancelAndJoin()

            assertTrue(job.isCancelled)
            assertNull(result)
        }

    @Test
    fun `블록이 Firebase가 아닌 예외를 던지면 Unknown 실패를 반환해야 한다`() =
        runTest {
            assertEquals(Outcome.Failure(AuthError.Unknown), toAuthResult { throw IOException("연결 끊김") })
        }
}
