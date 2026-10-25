package com.codealphas.themovie.data.review.local

// ReviewDao 쿼리가 이 이름을 'SYNCED' 같은 문자열로 직접 쓰므로, 이름을 바꾸면 ReviewDao 쿼리도 함께 수정
// 출시 뒤에 이름만 바꾸면 기기에 예전 이름으로 저장된 행을 Room이 읽지 못하고 예외를 던지므로,
// 저장된 값을 새 이름으로 고치는 마이그레이션과 함께 이름 수정
internal enum class ReviewSyncState {
    /** 서버에 올린 내용과 같다. */
    SYNCED,

    /** 새로 쓰거나 고친 내용을 아직 서버에 올리지 않았다. */
    PENDING_UPSERT,

    /** 지운 감상문이고, 서버에서 지우는 처리가 아직 끝나지 않았다. */
    PENDING_DELETE,
}
