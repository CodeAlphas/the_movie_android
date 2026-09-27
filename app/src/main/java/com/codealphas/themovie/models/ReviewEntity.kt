package com.codealphas.themovie.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviewTable")
class ReviewEntity(
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
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
)
