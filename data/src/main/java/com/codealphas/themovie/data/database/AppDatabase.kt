package com.codealphas.themovie.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.codealphas.themovie.data.review.local.ReviewDao
import com.codealphas.themovie.data.review.local.ReviewEntity

@Database(entities = [ReviewEntity::class], version = 1)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
}
