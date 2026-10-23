package com.codealphas.themovie.data.review.sync

internal interface ReviewSyncScheduler {
    /**
     * 대기 중인 감상문을 서버에 올리는 작업을 예약한다.
     * 작업은 네트워크가 연결된 뒤 실행되고, 앱이 종료되거나 기기가 재시작돼도 남는다.
     * 이미 예약되거나 실행 중인 작업이 있으면 그 작업이 끝난 뒤에 이어서 실행한다.
     */
    fun schedule()
}
