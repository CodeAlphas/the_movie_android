package com.codealphas.themovie.data.review.remote

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseReviewStorageDataSource
    @Inject
    constructor(
        private val storage: FirebaseStorage,
    ) : ReviewStorageDataSource {
        override suspend fun delete(fileName: String) {
            storage.reference
                .child("review/photo")
                .child(fileName)
                .delete()
                .await()
        }
    }
