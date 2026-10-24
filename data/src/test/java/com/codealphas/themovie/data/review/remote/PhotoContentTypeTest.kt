package com.codealphas.themovie.data.review.remote

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhotoContentTypeTest {
    private val contentResolver: ContentResolver =
        ApplicationProvider.getApplicationContext<Context>().contentResolver

    @Test
    fun `카메라로 찍은 jpg 파일이면 image jpeg를 돌려줘야 한다`() {
        val uri = Uri.parse("file:///data/cache/review_camera/review_photo_1.jpg")

        assertEquals("image/jpeg", photoContentType(uri, contentResolver))
    }

    @Test
    fun `파일 확장자가 대문자여도 형식을 찾아야 한다`() {
        val uri = Uri.parse("file:///data/cache/review_camera/review_photo_1.JPG")

        assertEquals("image/jpeg", photoContentType(uri, contentResolver))
    }

    @Test
    fun `갤러리에서 고른 png 사진이면 ContentResolver가 알려 준 형식을 돌려줘야 한다`() {
        Robolectric.buildContentProvider(FakePhotoProvider::class.java).create(PHOTO_AUTHORITY)
        val uri = Uri.parse("content://$PHOTO_AUTHORITY/photo/1")

        assertEquals("image/png", photoContentType(uri, contentResolver))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `이미지가 아닌 파일이면 업로드 전에 실패해야 한다`() {
        photoContentType(Uri.parse("file:///data/cache/review_camera/note.txt"), contentResolver)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `확장자가 없는 파일이면 업로드 전에 실패해야 한다`() {
        photoContentType(Uri.parse("file:///data/cache/review_camera/review_photo"), contentResolver)
    }
}

private const val PHOTO_AUTHORITY = "com.codealphas.themovie.test.photo"

// Photo Picker처럼 사진 uri의 형식을 알려 주는 provider
class FakePhotoProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "image/png"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
