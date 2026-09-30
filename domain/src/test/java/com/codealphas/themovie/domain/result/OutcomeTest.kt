package com.codealphas.themovie.domain.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class OutcomeTest {
    @Test
    fun `성공에 map을 적용하면 변환한 값을 Success로 반환해야 한다`() {
        val outcome: Outcome<Int, String> = Outcome.Success(2)

        assertEquals(Outcome.Success("4"), outcome.map { (it * 2).toString() })
    }

    @Test
    fun `실패에 map을 적용하면 변환하지 않고 같은 Failure를 반환해야 한다`() {
        val outcome: Outcome<Int, String> = Outcome.Failure("오류")
        var transformed = false

        val mapped =
            outcome.map {
                transformed = true
                it * 2
            }

        assertSame(outcome, mapped)
        assertEquals(false, transformed)
    }

    @Test
    fun `성공이면 onSuccess만 실행하고 같은 값을 반환해야 한다`() {
        val outcome: Outcome<Int, String> = Outcome.Success(1)
        val calls = mutableListOf<String>()

        val returned =
            outcome
                .onSuccess { calls += "onSuccess $it" }
                .onFailure { calls += "onFailure $it" }

        assertEquals(listOf("onSuccess 1"), calls)
        assertSame(outcome, returned)
    }

    @Test
    fun `실패면 onFailure만 실행하고 같은 값을 반환해야 한다`() {
        val outcome: Outcome<Int, String> = Outcome.Failure("오류")
        val calls = mutableListOf<String>()

        val returned =
            outcome
                .onSuccess { calls += "onSuccess $it" }
                .onFailure { calls += "onFailure $it" }

        assertEquals(listOf("onFailure 오류"), calls)
        assertSame(outcome, returned)
    }
}
