package com.codealphas.themovie.data.review.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(review: ReviewEntity)

    @Delete
    suspend fun delete(review: ReviewEntity)

    @Update
    suspend fun update(review: ReviewEntity)

    @Query("SELECT * FROM reviewTable WHERE id = :id")
    suspend fun getById(id: String): ReviewEntity?

    @Query("DELETE FROM reviewTable")
    suspend fun deleteAll()

    // 수정한 감상문이 맨 위로 오도록 updatedAt 순으로 정렬하고, 같은 시각이면 작성 순서인 push 키 순서로 고정
    @Query("SELECT * FROM reviewTable ORDER BY updatedAt DESC, id DESC")
    fun getAll(): Flow<List<ReviewEntity>>

    // 업로드가 중간에 멈추면 남은 감상문은 다음 실행으로 미뤄지므로, 오래 기다린 변경부터 올리도록 updatedAt 오름차순으로 정렬
    @Query("SELECT * FROM reviewTable WHERE syncState != 'SYNCED' ORDER BY updatedAt ASC, id ASC")
    suspend fun getPending(): List<ReviewEntity>

    // 업로드하는 동안 다시 수정되거나 삭제 표시된 감상문을 SYNCED로 덮어쓰면 그 변경이 서버에 올라가지 않으므로,
    // 올린 localVersion 그대로 PENDING_UPSERT일 때만 SYNCED로 변경
    @Query(
        "UPDATE reviewTable SET syncState = 'SYNCED' " +
            "WHERE id = :id AND localVersion = :localVersion AND syncState = 'PENDING_UPSERT'",
    )
    suspend fun markSynced(
        id: String,
        localVersion: Int,
    ): Int

    // 삭제 표시가 없는 감상문의 id가 잘못 넘어오면 사용자가 지운 적 없는 감상문이 목록에서 사라지므로,
    // PENDING_DELETE 감상문일 때만 행 삭제
    @Query("DELETE FROM reviewTable WHERE id = :id AND syncState = 'PENDING_DELETE'")
    suspend fun deleteIfPendingDelete(id: String): Int
}
