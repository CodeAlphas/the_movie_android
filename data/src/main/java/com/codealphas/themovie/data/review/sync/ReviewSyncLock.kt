package com.codealphas.themovie.data.review.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버 감상문을 Room에 넣는 동기화와 서버에서 감상문을 지우는 업로드가 서로 끼어들지 않도록 순서를 맞춘다.
 * 감상문 목록 화면의 동기화와 WorkManager 업로드 작업이 같은 인스턴스를 쓰도록 앱에 하나만 만든다.
 */
@Singleton
internal class ReviewSyncLock
    @Inject
    constructor() {
        private val mutex = Mutex()

        suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
    }
