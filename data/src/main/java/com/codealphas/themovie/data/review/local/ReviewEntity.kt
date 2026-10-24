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
    @ColumnInfo(name = "createdAt")
    val createdAt: Long,
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long,
    @ColumnInfo(name = "rating")
    val rating: Double,
    @ColumnInfo(name = "storageFileName")
    val storageFileName: String,
    // 사진을 바꿔 저장해도 감상문 업로드가 끝날 때까지 서버 감상문은 이전 사진을 가리키므로,
    // 감상문 업로드가 끝난 뒤 Storage에서 지울 이전 사진 파일 이름을 보관하고 지울 파일이 없으면 빈 문자열 적용
    @ColumnInfo(name = "storageFileNameToDelete")
    val storageFileNameToDelete: String,
    @ColumnInfo(name = "syncState")
    val syncState: ReviewSyncState,
    @ColumnInfo(name = "localVersion")
    val localVersion: Int,
    // 기기마다 1부터 매기는 자동 증가 id를 서버 키로 쓰면 재설치 뒤나 다른 기기에서 서버 감상문을 덮어쓰므로,
    // 기기끼리 겹치지 않고 문자열 순서가 작성 순서인 Firebase push 키를 Room id와 서버 키로 함께 사용
    @PrimaryKey
    val id: String,
)
