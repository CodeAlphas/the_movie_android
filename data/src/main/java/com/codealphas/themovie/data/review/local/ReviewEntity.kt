package com.codealphas.themovie.data.review.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviewTable")
internal class ReviewEntity(
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "image")
    val image: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "time")
    val time: String,
    @ColumnInfo(name = "rating")
    val rating: Double,
    @ColumnInfo(name = "storageFileName")
    val storageFileName: String,
    // 기기마다 1부터 자동 증가하는 id를 서버 키로도 쓰므로, 한 계정을 여러 기기에서 쓰면 id가 겹쳐 덮어써질 수 있어 단일 기기 사용을 전제로 허용
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
)
