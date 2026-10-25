package com.codealphas.themovie.data.review.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkManagerReviewSyncSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scheduler = WorkManagerReviewSyncScheduler(context)

    // 단위 테스트에서는 WorkManager가 초기화되지 않아 예약할 때 IllegalStateException이 나므로, WorkManagerTestInitHelper로 초기화
    @Before
    fun setUp() {
        val configuration = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
    }

    @After
    fun tearDown() {
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    @Test
    fun `예약한 업로드 작업을 취소하면 작업이 CANCELLED가 되어야 한다`() {
        scheduler.schedule()

        scheduler.cancel()

        assertEquals(listOf(WorkInfo.State.CANCELLED), workStates())
    }

    @Test
    fun `업로드 작업을 취소한 뒤 다시 예약하면 취소된 작업을 지우고 새 작업을 대기시켜야 한다`() {
        scheduler.schedule()
        scheduler.cancel()

        scheduler.schedule()

        // WorkManagerTestInitHelper로 초기화한 WorkManager는 네트워크 조건이 충족됐다고 알리기 전까지 작업을 실행하지 않으므로, 새 작업이 대기 상태로 남았는지 확인
        assertEquals(listOf(WorkInfo.State.ENQUEUED), workStates())
    }

    private fun workStates(): List<WorkInfo.State> =
        WorkManager
            .getInstance(context)
            .getWorkInfosForUniqueWork(WorkManagerReviewSyncScheduler.UNIQUE_WORK_NAME)
            .get()
            .map { it.state }
}
