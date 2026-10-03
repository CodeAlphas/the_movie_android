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
    // 기기마다 1부터 매기는 자동 증가 id를 서버 키로 쓰면 재설치 뒤나 다른 기기에서 서버 감상문을 덮어쓰므로,
    // 기기끼리 겹치지 않고 문자열 순서가 작성 순서인 Firebase push 키를 Room id와 서버 키로 함께 사용
    @PrimaryKey
    val id: String,
)
