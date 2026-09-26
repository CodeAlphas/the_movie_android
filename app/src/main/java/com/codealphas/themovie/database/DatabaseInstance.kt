package com.codealphas.themovie.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.codealphas.themovie.models.Review

@Database(entities = [Review::class], version = 1)
abstract class DatabaseInstance : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile
        // 영화 감상문 정보를 관리하기 위한 RoomDatabase 객체
        private var instance: DatabaseInstance? = null

        fun getInstance(context: Context): DatabaseInstance =
            instance ?: synchronized(this) {
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            DatabaseInstance::class.java,
                            "review_database",
                        ).allowMainThreadQueries()
                        .build()
                this.instance = instance
                instance
            }
    }
} // 싱글톤 디자인 패턴
