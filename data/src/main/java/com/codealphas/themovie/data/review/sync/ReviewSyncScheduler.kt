package com.codealphas.themovie.data.review.sync

internal interface ReviewSyncScheduler {
    /**
     * 대기 중인 감상문을 서버에 올리는 작업을 예약한다.
     * 작업은 네트워크가 연결된 뒤 실행되고, 앱이 종료되거나 기기가 재시작돼도 남는다.
     * 이미 예약되거나 실행 중인 작업이 있으면 그 작업이 끝난 뒤에 이어서 실행한다.
     */
    fun schedule()

    /**
     * 예약되거나 실행 중인 업로드 작업을 취소한다.
     * 실행 중인 작업은 멈추라는 신호만 받으므로, 취소 전에 보낸 서버 요청은 반영될 수 있다.
     * 취소한 뒤 [schedule]을 부르면 취소된 작업에 이어 붙지 않고 새 작업으로 예약한다.
     */
    fun cancel()
}
