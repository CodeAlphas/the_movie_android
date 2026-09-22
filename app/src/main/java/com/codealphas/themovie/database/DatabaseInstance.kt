package com.example.movieapplication.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.movieapplication.models.Review

@Database(entities = [Review::class], version = 1)
abstract class DatabaseInstance : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile
        private var INSTANCE: DatabaseInstance? = null // 영화 감상문 정보를 관리하기 위한 RoomDatabase 객체

        fun getInstance(context: Context): DatabaseInstance {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DatabaseInstance::class.java,
                    "review_database"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }
    }
} // 싱글톤 디자인 패턴