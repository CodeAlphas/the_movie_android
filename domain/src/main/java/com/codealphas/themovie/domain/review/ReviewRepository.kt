package com.codealphas.themovie.domain.review

import com.codealphas.themovie.domain.result.Outcome
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun observeAll(): Flow<List<Review>>

    /**
     * 서버에 저장된 감상문을 Room으로 가져온다.
     * Room에 이미 있는 id는 서버 값으로 덮어쓰지 않고, 서버에만 있는 감상문만 추가한다.
     *
     * @return 로그인 정보가 없거나 가져오기에 실패하면 [Outcome.Failure]
     */
    suspend fun syncFromRemote(): ReviewResult

    suspend fun getById(id: Int): Review?

    /**
     * 감상문을 Room과 서버에 저장한다. [ReviewDraft.id]가 null이면 새로 추가하고, 있으면 기존 감상문을 수정한다.
     * 새 사진은 먼저 업로드한 뒤 Room, 서버 순서로 저장한다.
     *
     * @return 사진 업로드에 실패하면 [ReviewError.PhotoUploadFailed]이며, 이때 기존 사진과 Room, 서버는 바뀌지 않는다.
     * 로그인 정보가 없거나 수정할 감상문이 Room에 없으면 저장하지 않고 [ReviewError.Unknown]으로 실패한다.
     */
    suspend fun save(draft: ReviewDraft): ReviewResult

    /**
     * 감상문을 Room, 서버, Storage 사진 순서로 지운다.
     * 서버 삭제는 요청만 하고 완료를 기다리지 않으므로, 서버에서 나중에 실패해도 결과에 반영되지 않는다.
     *
     * @return Room에서 지우지 못하면 [ReviewError.Unknown]으로 실패하며, 이때 서버와 사진은 지우지 않는다.
     * Room에서 지웠으면 서버 삭제 요청이나 사진 삭제가 실패해도 성공이다. 서버 삭제 요청이 실패하면 사진은 지우지 않는다.
     * 로그인 정보가 없으면 Room에서만 지우고 성공이다.
     */
    suspend fun delete(review: Review): ReviewResult

    suspend fun deleteAll()
}
