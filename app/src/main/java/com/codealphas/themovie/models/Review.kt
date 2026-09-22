package com.example.movieapplication.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// 영화 감상문 정보를 저장하기 위한 테이블
@Entity(tableName = "reviewTable")
class Review(
    @ColumnInfo(name = "title") // 감상문 제목
    val title: String,
    @ColumnInfo(name = "image") // 감상문에 포함될 이미지 저장 경로
    val image: String,
    @ColumnInfo(name = "content") // 감상문 내용
    val content: String,
    @ColumnInfo(name = "time") // 감상문 작성/업데이트 시간
    val time: String,
    @ColumnInfo(name = "rating") // 영화 개인 평점
    val rating: Double,
    @ColumnInfo(name = "storageFileName") // Firebase Storage에 저장된 이미지 파일 이름
    val storageFileName: String
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0 // 감상문 id
}