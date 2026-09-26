package com.codealphas.themovie.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.codealphas.themovie.models.Review

@Database(entities = [Review::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
}
