package com.example.movieapplication.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.movieapplication.models.Review

// 영화 감상문 정보 테이블에 접근할 수 있는 메소드를 선언해 놓은 인터페이스
@Dao
interface ReviewDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(review: Review)

    @Delete
    suspend fun delete(review: Review)

    @Update
    suspend fun update(review: Review)

    @Transaction
    suspend fun insertTransaction(review: Review): Int {
        insert(review)
        return getMaxId()
    }

    @Query("SELECT MAX(id) FROM reviewTable")
    suspend fun getMaxId(): Int

    @Query("DELETE FROM reviewTable")
    fun deleteAll()

    @Query("SELECT * FROM reviewTable ORDER BY id DESC")
    fun getAll(): LiveData<List<Review>>
}
