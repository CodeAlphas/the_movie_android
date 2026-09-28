package com.codealphas.themovie.data.review.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(review: ReviewEntity)

    @Delete
    suspend fun delete(review: ReviewEntity)

    @Update
    suspend fun update(review: ReviewEntity)

    @Transaction
    suspend fun insertTransaction(review: ReviewEntity): Int {
        insert(review)
        return getMaxId()
    }

    @Query("SELECT MAX(id) FROM reviewTable")
    suspend fun getMaxId(): Int

    @Query("DELETE FROM reviewTable")
    suspend fun deleteAll()

    @Query("SELECT * FROM reviewTable ORDER BY id DESC")
    fun getAll(): Flow<List<ReviewEntity>>
}
