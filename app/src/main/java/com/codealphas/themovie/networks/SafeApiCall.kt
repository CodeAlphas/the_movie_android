package com.codealphas.themovie.networks

import com.codealphas.themovie.domain.DataResult
import com.codealphas.themovie.domain.RemoteError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

suspend fun <T> safeApiCall(block: suspend () -> T): DataResult<T> =
    try {
        DataResult.Success(block())
    } catch (e: CancellationException) {
        // 취소를 Failure로 바꾸면 ViewModel이 사라진 뒤에도 뒤따르는 코드가 실행되므로, 호출한 코루틴이 멈추도록 취소 예외를 다시 던짐
        throw e
    } catch (_: SocketTimeoutException) {
        DataResult.Failure(RemoteError.Timeout)
    } catch (_: IOException) {
        DataResult.Failure(RemoteError.Network)
    } catch (e: HttpException) {
        DataResult.Failure(RemoteError.Http(e.code()))
    } catch (_: SerializationException) {
        DataResult.Failure(RemoteError.Serialization)
    } catch (_: Exception) {
        DataResult.Failure(RemoteError.Unknown)
    }
