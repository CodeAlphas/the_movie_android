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
