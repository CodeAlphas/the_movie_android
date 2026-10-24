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
     * 서버 응답을 일정 시간까지만 기다리므로, 오프라인이어도 실패로 끝난다.
     * 사용자가 지운 감상문은 서버 삭제가 끝나기 전이든 후든 가져오기로 다시 추가되지 않는다.
     * 이를 위해 그 감상문을 서버에서 지우는 중이면 삭제가 끝난 뒤에 가져온다.
     * 이 기다림도 서버 응답을 기다리는 일정 시간에 포함되므로, 삭제가 그 시간 안에 끝나지 않으면 실패로 끝난다.
     *
     * @return 로그인 정보가 없거나 가져오기에 실패하거나 서버 응답이 시간 안에 오지 않으면 [Outcome.Failure]
     */
    suspend fun syncFromRemote(): ReviewResult

    suspend fun getById(id: String): Review?

    /**
     * 감상문을 Room에 저장하고 감상문 업로드를 예약한다. [ReviewDraft.id]가 null이면 새로 추가하고, 있으면 기존 감상문을 수정한다.
     * 새 사진은 먼저 Storage에 업로드한 뒤 Room에 저장한다.
     * 사진 업로드 중에 호출한 코루틴이 취소되면 저장하지 않지만, Room 저장을 시작한 뒤에 취소되면 저장을 끝까지 진행한다.
     * 사진 업로드에 실패하면 기존 사진과 Room을 바꾸지 않는다.
     * 바꾸거나 지운 이전 사진 파일은 저장할 때 지우지 않고 감상문 업로드가 끝난 뒤에 지운다.
     * 다만 감상문 업로드가 끝나기 전에 사진을 다시 바꾸면, 그 사이에 골랐던 사진은 저장할 때 바로 지운다.
     * 그 사진을 올리던 감상문 업로드가 진행 중이었다면, 다음 감상문 업로드 전까지 서버 감상문이 지운 사진을 가리킬 수 있다.
     * 감상문 업로드는 연결되면 백그라운드에서 진행하므로, 업로드 결과는 반환값에 반영되지 않는다.
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

    /**
     * 서버에 아직 반영하지 않은 감상문 변경(작성, 수정, 삭제) 수를 돌려준다.
     * 서버가 거부해 대기 상태로 남은 변경도 센다.
     */
    suspend fun countPendingChanges(): Int

    /**
     * 예약되거나 실행 중인 서버 업로드 작업을 취소한다. 대기 중인 감상문 변경은 Room에 그대로 남는다.
     * 실행 중인 작업은 멈추라는 신호만 받으므로, 취소 전에 보낸 서버 요청은 반영될 수 있다.
     */
    fun cancelPendingSync()
}
