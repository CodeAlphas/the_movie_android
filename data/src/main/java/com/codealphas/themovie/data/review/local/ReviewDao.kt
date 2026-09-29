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
    // id가 INTEGER PRIMARY KEY라 반환되는 rowId가 저장된 감상문 id와 같고, IGNORE로 건너뛴 행은 -1
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(review: ReviewEntity): Long

    @Delete
    suspend fun delete(review: ReviewEntity)

    @Update
    suspend fun update(review: ReviewEntity)

    @Query("SELECT * FROM reviewTable WHERE id = :id")
    suspend fun getById(id: Int): ReviewEntity?

    @Query("DELETE FROM reviewTable")
    suspend fun deleteAll()

    @Query("SELECT * FROM reviewTable ORDER BY id DESC")
    fun getAll(): Flow<List<ReviewEntity>>
}
