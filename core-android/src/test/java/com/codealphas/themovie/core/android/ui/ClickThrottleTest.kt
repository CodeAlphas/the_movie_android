package com.codealphas.themovie.core.android.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickThrottleTest {
    private var currentTime = 0L
    private val throttle = ClickThrottle(windowMs = 500L, now = { currentTime })

    @Test
    fun `처음 누르면 클릭을 받아야 한다`() {
        assertTrue(throttle.tryAcquire())
    }

    @Test
    fun `누른 뒤 500ms 안에 다시 누르면 두 번째 클릭을 막아야 한다`() {
        throttle.tryAcquire()
        currentTime = 499L

        assertFalse(throttle.tryAcquire())
    }

    @Test
    fun `누른 뒤 500ms가 지나 다시 누르면 두 번째 클릭을 받아야 한다`() {
        throttle.tryAcquire()
        currentTime = 500L

        assertTrue(throttle.tryAcquire())
    }

    @Test
    fun `막힌 클릭이 있어도 처음 누른 뒤 500ms가 지나 누르면 클릭을 받아야 한다`() {
        throttle.tryAcquire()
        currentTime = 400L
        throttle.tryAcquire()
        currentTime = 500L

        // 막힌 400ms 클릭이 아니라 받아들인 0ms 클릭부터 창을 세는지 확인
        assertTrue(throttle.tryAcquire())
    }
}
