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
}
