package com.codealphas.themovie.data.review.remote

internal interface ReviewStorageDataSource {
    suspend fun delete(fileName: String)
}
