package com.codealphas.themovie.data.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal class RetryInterceptor(
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) },
    private val random: () -> Double = { java.lang.Math.random() },
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        while (true) {
            throwIfCanceled(chain)
            val response = chain.proceed(request)
            val delayMs =
                if (shouldRetry(request, response) && attempt < MAX_RETRIES) {
                    retryDelayMs(response, attempt)
                } else {
                    null
                }
            if (delayMs == null) {
                return response
            }
            // 응답 본문을 닫지 않으면 연결이 풀로 돌아가지 않으므로, 다시 요청하기 전에 연결을 반환
            response.close()
            throwIfCanceled(chain)
            sleep(delayMs)
            throwIfCanceled(chain)
            attempt++
        }
    }

    // POST는 응답이 유실돼도 서버에 반영됐을 수 있으므로, GET과 HEAD만 다시 요청
    private fun shouldRetry(
        request: Request,
        response: Response,
    ): Boolean = request.method in RETRYABLE_METHODS && response.code in RETRYABLE_STATUS_CODES

    private fun retryDelayMs(
        response: Response,
        attempt: Int,
    ): Long? {
        val header = response.header("Retry-After") ?: return backoffDelayMs(attempt)
        val seconds = header.trim().toLongOrNull()
        // Retry-After가 5초를 넘거나 날짜 형식이면 그만큼 기다리는 동안 화면이 멈추므로, 이때는 다시 요청하지 않고 응답 반환
        return if (seconds != null && seconds in 0L..MAX_RETRY_AFTER_SECONDS) {
            seconds * MILLIS_PER_SECOND
        } else {
            null
        }
    }

    private fun backoffDelayMs(attempt: Int): Long {
        val interval = INITIAL_INTERVAL_MS * (1L shl attempt)
        val jitter = (random() * (JITTER_MS + 1)).toLong().coerceIn(0L, JITTER_MS)
        return interval + jitter
    }

    // 화면을 나간 뒤에도 대기와 재요청이 이어지므로, 취소된 호출은 재시도에서 제외
    private fun throwIfCanceled(chain: Interceptor.Chain) {
        if (chain.call().isCanceled()) {
            throw IOException("Canceled")
        }
    }

    private fun sleep(delayMs: Long) {
        try {
            sleeper(delayMs)
        } catch (e: InterruptedException) {
            // Thread.sleep이 끊기면 OkHttp는 IOException만 실패로 받으므로, 중단 상태를 남긴 뒤 IOException으로 전달
            Thread.currentThread().interrupt()
            throw IOException("Canceled", e)
        }
    }

    private companion object {
        const val MAX_RETRIES = 2
        const val INITIAL_INTERVAL_MS = 1_000L
        const val JITTER_MS = 500L
        const val MAX_RETRY_AFTER_SECONDS = 5L
        const val MILLIS_PER_SECOND = 1_000L
        val RETRYABLE_METHODS = setOf("GET", "HEAD")
        val RETRYABLE_STATUS_CODES =
            setOf(
                408, // 요청 시간 초과
                429, // 너무 많은 요청
                500, // 내부 서버 오류
                502, // 불량 게이트웨이
                503, // 서비스를 사용할 수 없음
                504, // 게이트웨이 시간 초과
            )
    }
}
