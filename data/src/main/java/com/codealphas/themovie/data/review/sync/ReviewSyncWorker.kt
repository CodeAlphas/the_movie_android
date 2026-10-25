package com.codealphas.themovie.data.review.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
internal class ReviewSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val uploader: ReviewUploader,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result =
            when (uploader.upload()) {
                ReviewUploadResult.SUCCESS -> Result.success()
                ReviewUploadResult.RETRY -> Result.retry()
                // failure()로 끝내면 WorkManager가 실행 중에 APPEND_OR_REPLACE로 이어 붙은 작업까지 실패 처리해
                // 그동안 저장한 감상문이 올라가지 않으므로, 거부된 감상문은 대기 상태로 두고 다음 예약에 맡기도록 성공 처리
                ReviewUploadResult.PERMANENT_FAILURE -> Result.success()
            }
    }
