package com.codealphas.themovie.data.network

import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.result.RemoteError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

internal suspend fun <T> safeApiCall(block: suspend () -> T): DataResult<T> =
    try {
        Outcome.Success(block())
    } catch (e: CancellationException) {
        // 취소를 Failure로 바꾸면 ViewModel이 사라진 뒤에도 뒤따르는 코드가 실행되므로, 호출한 코루틴이 멈추도록 취소 예외를 그대로 전파
        throw e
    } catch (_: SocketTimeoutException) {
        Outcome.Failure(RemoteError.Timeout)
    } catch (_: IOException) {
        Outcome.Failure(RemoteError.Network)
    } catch (e: HttpException) {
        Outcome.Failure(RemoteError.Http(e.code()))
    } catch (_: SerializationException) {
        Outcome.Failure(RemoteError.Serialization)
    } catch (_: Exception) {
        Outcome.Failure(RemoteError.Unknown)
    }
