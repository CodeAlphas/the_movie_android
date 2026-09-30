package com.codealphas.themovie.data.auth

import com.codealphas.themovie.domain.auth.AuthError
import com.codealphas.themovie.domain.result.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

// Firebase 예외 생성자가 부르는 TextUtils.isEmpty는 JVM 단위 테스트의 android.jar에서 실제 동작 대신 not mocked 예외를 던지므로,
// Firebase 예외 종류별 AuthError 매핑 검증은 이 테스트에서 제외
class AuthErrorMapperTest {
    @Test
    fun `블록이 끝나면 Success를 반환해야 한다`() =
        runTest {
            assertEquals(Outcome.Success(Unit), toAuthResult { "uid" })
        }

    @Test
    fun `블록이 취소 예외를 던지면 실패로 바꾸지 않고 같은 예외를 다시 던져야 한다`() =
        runTest {
            val cancellation = CancellationException("화면 종료")

            try {
                toAuthResult { throw cancellation }
                fail("취소 예외가 다시 던져지지 않음")
            } catch (thrown: CancellationException) {
                assertSame(cancellation, thrown)
            }
        }

    @Test
    fun `블록이 Firebase가 아닌 예외를 던지면 Unknown 실패를 반환해야 한다`() =
        runTest {
            assertEquals(Outcome.Failure(AuthError.Unknown), toAuthResult { throw IOException("연결 끊김") })
        }
}
