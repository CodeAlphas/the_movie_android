package com.codealphas.themovie.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// 영화 감상문 정보를 저장하기 위한 테이블
@Entity(tableName = "reviewTable")
class Review(
    // 감상문 제목
    @ColumnInfo(name = "title")
    val title: String,
    // 감상문에 포함될 이미지 저장 경로
    @ColumnInfo(name = "image")
    val image: String,
    // 감상문 내용
    @ColumnInfo(name = "content")
    val content: String,
    // 감상문 작성/업데이트 시간
    @ColumnInfo(name = "time")
    val time: String,
    // 영화 개인 평점
    @ColumnInfo(name = "rating")
    val rating: Double,
    // Firebase Storage에 저장된 이미지 파일 이름
    @ColumnInfo(name = "storageFileName")
    val storageFileName: String,
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0 // 감상문 id
}
