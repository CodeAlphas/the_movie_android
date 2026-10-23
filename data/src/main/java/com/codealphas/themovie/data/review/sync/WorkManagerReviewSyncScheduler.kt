package com.codealphas.themovie.data.review.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class WorkManagerReviewSyncScheduler
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : ReviewSyncScheduler {
        override fun schedule() {
            val request =
                OneTimeWorkRequestBuilder<ReviewSyncWorker>()
                    // 오프라인에서 실행하면 업로드가 실패해 재시도만 반복하므로, 네트워크가 연결된 뒤 실행하도록 조건 적용
                    .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                    .build()
            // REPLACE는 실행 중인 업로드를 중간에 취소하므로 제외
            // KEEP은 실행 중인 작업이 있으면 새 작업을 버려, 업로드 중에 저장한 감상문이 올라가지 않을 수 있으므로 제외
            // APPEND는 앞 작업이 실패로 끝나면 새 작업도 실행하지 않고 실패 처리하므로,
            // 실패한 앞 작업을 버리고 새 작업을 실행하도록 APPEND_OR_REPLACE 적용
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        private companion object {
            const val UNIQUE_WORK_NAME = "review_sync"
        }
    }
