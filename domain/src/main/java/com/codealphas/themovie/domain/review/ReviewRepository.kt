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

    suspend fun getById(id: Int): Review?

    /**
     * 감상문을 Room과 서버에 저장한다. [ReviewDraft.id]가 null이면 새로 추가하고, 있으면 기존 감상문을 수정한다.
     * 새 사진은 먼저 업로드한 뒤 Room, 서버 순서로 저장한다.
     * 사진 업로드 중에 호출한 코루틴이 취소되면 저장하지 않지만, Room 저장을 시작한 뒤에 취소되면 저장을 끝까지 진행한다.
     * 사진 업로드에 실패하면 기존 사진과 Room, 서버를 바꾸지 않는다.
     * 서버 저장은 요청만 하고 완료를 기다리지 않으므로, 서버에서 나중에 실패해도 결과에 반영되지 않는다.
     *
     * @return 사진 업로드에 실패하면 [ReviewError.PhotoUploadFailed],
     * 로그인 정보가 없거나 수정할 감상문이 Room에 없거나 Room 저장에 실패하면 [ReviewError.Unknown]
     */
    suspend fun save(draft: ReviewDraft): ReviewResult

    /**
     * 감상문을 Room, 서버, Storage 사진 순서로 지운다.
     * Room에서 지우지 못하면 서버와 사진은 지우지 않고, 로그인 정보가 없으면 Room에서만 지운다.
     * 서버 삭제는 요청만 하고 완료를 기다리지 않으므로, 서버에서 나중에 실패해도 결과에 반영되지 않는다.
     * 서버 삭제 요청을 보내지 못하면 사진은 지우지 않는다.
     *
     * @return Room에서 지우지 못했을 때만 [ReviewError.Unknown]. 서버 삭제 요청이나 사진 삭제가 실패해도 성공
     */
    suspend fun delete(review: Review): ReviewResult

    suspend fun deleteAll()
}
