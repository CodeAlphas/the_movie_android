package com.codealphas.themovie.domain.review

import com.codealphas.themovie.domain.result.Outcome
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun observeAll(): Flow<List<Review>>

    /**
     * 서버에 저장된 감상문을 Room으로 가져온다.
     * 같은 id의 감상문이 Room에 이미 있으면 서버 감상문으로 바꾸지 않고 그대로 두며, Room에 없는 감상문만 추가한다.
     * 가져오는 중에 실패하면 그때까지 추가한 감상문은 Room에 남는다.
     *
     * @return 로그인 정보가 없거나 가져오기에 실패하면 [Outcome.Failure]
     */
    suspend fun syncFromRemote(): ReviewResult

    suspend fun getById(id: String): Review?

    /**
     * 감상문을 Room에 저장하고 서버 업로드를 예약한다. [ReviewDraft.id]가 null이면 새로 추가하고, 있으면 기존 감상문을 수정한다.
     * 새 사진은 먼저 업로드한 뒤 Room에 저장한다.
     * 사진 업로드 중에 호출한 코루틴이 취소되면 저장하지 않지만, Room 저장을 시작한 뒤에 취소되면 저장을 끝까지 진행한다.
     * 사진 업로드에 실패하면 기존 사진과 Room을 바꾸지 않는다.
     * 서버 업로드는 연결되면 백그라운드에서 진행하므로, 업로드 결과는 반환값에 반영되지 않는다.
     *
     * @return 사진 업로드에 실패하면 [ReviewError.PhotoUploadFailed],
     * 로그인 정보가 없거나 수정할 감상문이 Room에 없거나(지운 감상문 포함) Room 저장에 실패하면 [ReviewError.Unknown]
     */
    suspend fun save(draft: ReviewDraft): ReviewResult

    /**
     * 감상문을 지운다. Room에 삭제 표시를 하고 서버와 Storage 사진 삭제를 예약하므로, 오프라인이어도 서버를 기다리지 않는다.
     * 지운 감상문은 서버에서 지워지기 전에도 [observeAll]과 [getById]에 나오지 않고, [syncFromRemote]로 다시 추가되지 않는다.
     *
     * @return Room에 삭제 표시를 하지 못하면 [ReviewError.Unknown]
     */
    suspend fun delete(review: Review): ReviewResult

    suspend fun deleteAll()
}
