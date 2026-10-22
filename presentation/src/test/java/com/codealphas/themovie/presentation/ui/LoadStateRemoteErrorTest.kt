package com.codealphas.themovie.presentation.ui

import androidx.paging.LoadState
import com.codealphas.themovie.domain.result.RemoteError
import com.codealphas.themovie.domain.result.RemoteErrorException
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadStateRemoteErrorTest {
    @Test
    fun `LoadState Error의 예외가 RemoteErrorException이면 담긴 RemoteError를 돌려줘야 한다`() {
        val state = LoadState.Error(RemoteErrorException(RemoteError.Timeout))

        assertEquals(RemoteError.Timeout, state.remoteError)
    }

    @Test
    fun `LoadState Error의 예외가 RemoteErrorException이 아니면 RemoteError Unknown을 돌려줘야 한다`() {
        val state = LoadState.Error(IllegalStateException())

        assertEquals(RemoteError.Unknown, state.remoteError)
    }
}
