package com.codealphas.themovie.review

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.codealphas.themovie.R
import com.codealphas.themovie.core.android.ui.applySystemBarInsets
import com.codealphas.themovie.core.android.ui.setupAppBar
import com.codealphas.themovie.databinding.ActivityReviewDetailBinding
import com.codealphas.themovie.domain.review.Review
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ReviewDetailActivity : AppCompatActivity() {
    private companion object {
        const val KEY_PHOTO_PATH = "photo_path"
        const val USER_ID_PREFIX_LENGTH = 10
    }

    private lateinit var binding: ActivityReviewDetailBinding
    private val viewModel: ReviewViewModel by viewModels()
    private lateinit var reviewDB: DatabaseReference
    private var photoFile: File? = null
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val userId: String by lazy { auth.currentUser?.uid.orEmpty() }
    private val storage: FirebaseStorage by lazy { Firebase.storage }
    private val reviewType: String? by lazy { intent.getStringExtra("reviewType") }
    private val cameraPermission: String by lazy { Manifest.permission.CAMERA }
    private var reviewId: Int = -1
    private var selectedImageUri: Uri? = null
    private var imageUri: String = ""
    private var fileName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 카메라 앱을 쓰는 동안 프로세스가 종료되면 촬영 파일을 잃어 결과를 받지 못하므로, 저장한 경로 복원
        photoFile = savedInstanceState?.getString(KEY_PHOTO_PATH)?.let(::File)
        applySystemBarInsets()

        binding = ActivityReviewDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAppBar(binding.toolbar, getString(R.string.review_edit_appbar_title))

        initViewContent()
        initTitleEditText()
        initImageView()
        initContentEditText()
        initSaveButton()
    }

    private fun initViewContent() {
        if (reviewType.equals("Edit")) {
            val reviewTitle = intent.getStringExtra("reviewTitle")
            val reviewImage = intent.getStringExtra("reviewImage")
            val reviewContent = intent.getStringExtra("reviewContent")
            val reviewRating = intent.getDoubleExtra("rating", 0.0)
            imageUri = reviewImage!!
            reviewId = intent.getIntExtra("reviewId", -1)
            fileName = intent.getStringExtra("storageFileName").toString()

            binding.titleEditText.setText(reviewTitle)
            if (reviewImage != "") {
                Glide
                    .with(this)
                    .load(reviewImage)
                    .centerCrop()
                    .error(R.drawable.set_image) // 원본이미지를 로드할 수 없을 때 보여줄 이미지 설정
                    .into(binding.imageView) // Firebase storage에 저장된 이미지 설정
            }
            binding.contentEditText.setText(reviewContent)
            binding.reviewRatingBar.rating = reviewRating.toFloat() / 2
            binding.button.text = getString(R.string.review_edit_update)
        } else {
            binding.button.text = getString(R.string.review_edit_create)
        }
    }

    private fun initTitleEditText() {
        binding.titleEditText.setOnKeyListener { view, i, keyEvent ->
            if ((keyEvent.action == KeyEvent.ACTION_DOWN) && (i == KeyEvent.KEYCODE_ENTER)) {
                hideKeyboard(this, binding.titleEditText)
                true
            } else {
                false
            }
        } // 사용자가 제목을 입력하고 enter키를 누르면 소프트 키보드를 화면에서 내림

        binding.titleEditText.setOnFocusChangeListener { view, b ->
            if (!b) {
                hideKeyboard(this, view)
            }
        }
    }

    private fun initImageView() {
        binding.imageView.setOnClickListener {
            showImageControlDialog()
        }
    }

    private fun showImageControlDialog() {
        AlertDialog
            .Builder(this)
            .setTitle(getString(R.string.review_edit_photo_dialog_title))
            .setMessage(getString(R.string.review_edit_photo_dialog_message))
            .setNeutralButton(getString(R.string.review_edit_photo_delete)) { _, _ ->
                deleteImage()
            }.setPositiveButton(getString(R.string.review_edit_photo_gallery)) { _, _ ->
                startGallery()
            }.setNegativeButton(getString(R.string.review_edit_photo_camera)) { _, _ ->
                startCamera()
            }.create()
            .show()
    }

    private fun deleteImage() {
        if (selectedImageUri != null) {
            binding.imageView.setImageResource(R.drawable.set_image)
            selectedImageUri = null
            imageUri = ""
        } else if (imageUri != "") {
            binding.imageView.setImageResource(R.drawable.set_image)
            imageUri = ""
        }
    }

    private fun startGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    } // 권한이 필요 없는 Photo Picker로 갤러리에서 사진을 선택

    private fun startCamera() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                cameraPermission,
            ) == PackageManager.PERMISSION_GRANTED -> {
                activateCamera()
            } // 카메라 사용 권한이 잘 부여되어있을 때, 카메라 실행
            shouldShowRequestPermissionRationale(cameraPermission) -> {
                showCameraPermissionPopup()
            } // 이전에 앱이 권한을 요청하고 사용자가 요청을 거부한 경우 교육용 팝업을 띄움
            else -> {
                cameraPermissionLauncher.launch(cameraPermission)
            } // 권한 요청을 위한 팝업을 띄움
        }
    }

    private fun showCameraPermissionPopup() {
        AlertDialog
            .Builder(this)
            .setTitle(getString(R.string.review_edit_permission_title))
            .setMessage(getString(R.string.review_edit_permission_camera))
            .setPositiveButton(getString(R.string.review_edit_permission_allow)) { _, _ ->
                cameraPermissionLauncher.launch(cameraPermission)
            }.setNegativeButton(getString(R.string.review_edit_permission_deny)) { _, _ -> }
            .create()
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        photoFile?.let { outState.putString(KEY_PHOTO_PATH, it.path) }
    }

    private fun activateCamera() {
        val file = File.createTempFile("review_photo_", ".jpg", externalCacheDir)
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        photoFile = file
        // 카메라 앱이 없는 기기에서는 실행 시 예외가 나므로, 앱 종료 대신 실패 안내 처리
        try {
            cameraLauncher.launch(uri)
        } catch (ignored: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.review_edit_photo_failed), Toast.LENGTH_SHORT).show()
        }
    } // 카메라를 실행시키고 촬영한 사진을 앱의 캐시 저장소에 저장해주는 메소드

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                activateCamera()
            } else {
                Toast.makeText(this, getString(R.string.review_edit_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // 선택 없이 닫은 경우는 실패가 아니므로 안내 없이 무시
            if (uri != null) {
                selectedImageUri = uri
                Glide
                    .with(this)
                    .load(uri)
                    .centerCrop()
                    .into(binding.imageView) // 사진을 올바르게 돌려서 imageView에 보여주기 위해 Glide 라이브러리 사용
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSaved ->
            val file = photoFile
            if (isSaved && file != null) {
                selectedImageUri = file.toUri()
                Glide
                    .with(this)
                    .load(file)
                    .centerCrop()
                    .into(binding.imageView) // 사진을 올바르게 돌려서 imageView에 보여주기 위해 Glide 라이브러리 사용
            } else {
                Toast.makeText(this, getString(R.string.review_edit_photo_failed), Toast.LENGTH_SHORT).show()
            }
        }

    private fun initContentEditText() {
        binding.contentEditText.setOnFocusChangeListener { view, b ->
            if (!b) {
                hideKeyboard(this, view)
            }
        }
    }

    private fun initSaveButton() {
        binding.button.setOnClickListener {
            dataChanged()
        }
    }

    private fun dataChanged() {
        val currentReviewTitle = binding.titleEditText.text.toString()
        val currentReviewContent = binding.contentEditText.text.toString()
        val currentRating = binding.reviewRatingBar.rating.toDouble() * 2

        // 입력 확인 전에 사진을 삭제하고 업로드하면 빈 입력으로 저장을 눌러도 사진만 바뀌므로, 입력 확인을 가장 먼저 처리
        if (currentReviewTitle.isBlank() || currentReviewContent.isBlank()) {
            Toast
                .makeText(applicationContext, getString(R.string.review_edit_input_required), Toast.LENGTH_LONG)
                .show()
            return
        }

        lifecycleScope.launch(Dispatchers.Main) {
            showProgress()
            val previousFileName = fileName
            val isPhotoReplaced = selectedImageUri != null
            val isPhotoRemoved = imageUri == "" && previousFileName != ""

            // 기존 사진을 먼저 지우는데 새 사진 업로드가 실패하면 사진이 사라지므로, 업로드 성공 뒤에 기존 사진 삭제
            if (isPhotoReplaced && !uploadPhotoToStorage()) {
                Toast
                    .makeText(applicationContext, getString(R.string.review_edit_upload_failed), Toast.LENGTH_LONG)
                    .show()
                hideProgress()
                return@launch
            }
            // 사진을 그대로 두는데 파일을 지우면 저장된 URL이 죽은 링크가 되므로,
            // 바꾸거나 지운 경우에만 기존 파일 삭제
            if ((isPhotoReplaced || isPhotoRemoved) && previousFileName != "") {
                val isDeleted = deletePhotoStorage(previousFileName)
                if (isPhotoRemoved && isDeleted) {
                    fileName = ""
                }
            }

            if (reviewType.equals("Edit")) {
                val updateReview =
                    Review(
                        title = currentReviewTitle,
                        image = imageUri,
                        content = currentReviewContent,
                        time = getCurrentDate(),
                        rating = currentRating,
                        storageFileName = fileName,
                        id = reviewId,
                    )

                viewModel.updateReview(updateReview)

                updateFirebaseRealtimeDB(
                    currentReviewTitle,
                    currentReviewContent,
                    currentRating,
                )

                Toast
                    .makeText(applicationContext, getString(R.string.review_edit_updated), Toast.LENGTH_LONG)
                    .show()
                finish()
            } else {
                val updateReview =
                    Review(
                        currentReviewTitle,
                        imageUri,
                        currentReviewContent,
                        getCurrentDate(),
                        currentRating,
                        fileName,
                    )

                viewModel.insertTransaction(updateReview)
                viewModel.maxId
                    .observe(
                        this@ReviewDetailActivity,
                        Observer { maxId ->
                            maxId?.let {
                                reviewId = it
                                updateFirebaseRealtimeDB(
                                    currentReviewTitle,
                                    currentReviewContent,
                                    currentRating,
                                )
                                Toast
                                    .makeText(
                                        applicationContext,
                                        getString(R.string.review_edit_created),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                finish()
                            }
                        },
                    )
            }
            hideProgress()
        }
    }

    private suspend fun deletePhotoStorage(targetFileName: String): Boolean =
        try {
            storage.reference
                .child("review/photo")
                .child(targetFileName)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e("ReviewDetailActivity", "Firebase 요청 실패", e)
            false
        }

    private suspend fun uploadPhotoToStorage(): Boolean {
        val uploadFileName =
            userId.substring(
                0,
                USER_ID_PREFIX_LENGTH,
            ) + "${System.currentTimeMillis()}.png" // Storage에 저장될 File의 이름을 지정
        return try {
            imageUri =
                storage.reference
                    .child("review/photo")
                    .child(uploadFileName)
                    .putFile(selectedImageUri!!)
                    .await()
                    .storage.downloadUrl
                    .await()
                    .toString()
            fileName = uploadFileName
            true
        } catch (e: Exception) {
            Log.e("ReviewDetailActivity", "Firebase 요청 실패", e)
            false
        }
    } // 이미지를 Firebase Storage의 지정된 경로에 업로드해주고 해당 이미지를 가져올 수 있는 Url을 반환해오는 메소드

    private fun updateFirebaseRealtimeDB(
        currentReviewTitle: String,
        currentReviewContent: String,
        currentRating: Double,
    ) {
        reviewDB =
            Firebase.database.reference
                .child("users")
                .child(userId)
                .child("reviews")
                .child(reviewId.toString())
        val review = mutableMapOf<String, Any>()
        review["id"] = reviewId
        review["image"] = imageUri
        review["title"] = currentReviewTitle
        review["content"] = currentReviewContent
        review["time"] = getCurrentDate()
        review["rating"] = currentRating
        review["storageFileName"] = fileName
        try {
            reviewDB.updateChildren(review)
        } catch (e: Exception) {
            Log.e("ReviewDetailActivity", "Firebase 요청 실패", e)
        }
    } // 서버에 감상문 정보를 저장(Firebase Realtime Database)해주는 메소드

    private fun showProgress() {
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) // 화면 터치 막기
        binding.progressBar.isVisible = true
    }

    private fun hideProgress() {
        binding.progressBar.isVisible = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) // 화면 터치 풀기
    }

    private fun hideKeyboard(
        context: Context,
        view: View,
    ) {
        val inputMethodManager =
            context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun getCurrentDate(): String {
        val time = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.KOREA)
        return time.format(Date())
    }
}
