package com.codealphas.themovie.data.review.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseReviewStorageDataSource
    @Inject
    constructor(
        private val storage: FirebaseStorage,
    ) : ReviewStorageDataSource {
        override suspend fun upload(
            fileName: String,
            uri: String,
        ): String =
            photoReference(fileName)
                .putFile(Uri.parse(uri))
                .await()
                .storage.downloadUrl
                .await()
                .toString()

        override suspend fun delete(fileName: String) {
            photoReference(fileName).delete().await()
        }

        private fun photoReference(fileName: String) =
            storage.reference
                .child("review/photo")
                .child(fileName)
    }
