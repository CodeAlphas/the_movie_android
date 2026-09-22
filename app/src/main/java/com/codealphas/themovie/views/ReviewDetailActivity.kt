package com.example.movieapplication.views

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.movieapplication.R
import com.example.movieapplication.databinding.ActivityReviewDetailBinding
import com.example.movieapplication.models.Review
import com.example.movieapplication.utils.Utils
import com.example.movieapplication.viewmodels.ReviewViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File

class ReviewDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewDetailBinding
    private lateinit var viewModel: ReviewViewModel
    private lateinit var reviewDB: DatabaseReference
    private lateinit var photoFile: File
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val userId: String by lazy { auth.currentUser?.uid.orEmpty() }
    private val storage: FirebaseStorage by lazy { Firebase.storage }
    private val reviewType: String? by lazy { intent.getStringExtra("reviewType") }
    private val readPermission: String by lazy { Manifest.permission.READ_EXTERNAL_STORAGE }
    private val cameraPermission: String by lazy { Manifest.permission.CAMERA }
    private var reviewId: Int = -1
    private var selectedImageUri: Uri? = null
    private var imageUri: String = ""
    private var fileName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReviewDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar!!.title = "영화 감상문 작성"

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
            binding.button.text = "감상문 수정" // 사용자가 감상문 편집 모드로 진입할 경우
        } else {
            binding.button.text = "감상문 등록" // 사용자가 감상문 등록 모드로 진입할 경우
        }
    }

    private fun initTitleEditText() {
        binding.titleEditText.setOnKeyListener { view, i, keyEvent ->
            if ((keyEvent.action == KeyEvent.ACTION_DOWN) && (i == KeyEvent.KEYCODE_ENTER)) {
                Utils.hideKeyboard(this, binding.titleEditText)
                true
            } else {
                false
            }
        } // 사용자가 제목을 입력하고 enter키를 누르면 소프트 키보드를 화면에서 내림

        binding.titleEditText.setOnFocusChangeListener { view, b ->
            if (!b) {
                Utils.hideKeyboard(this, view)
            }
        }
    }

    private fun initImageView() {
        binding.imageView.setOnClickListener {
            showImageControlDialog()
        }
    }

    private fun showImageControlDialog() {
        AlertDialog.Builder(this)
            .setTitle("사진첨부 및 삭제")
            .setMessage("사진을 첨부하거나 이미 첨부한 사진을 삭제하세요.")
            .setNeutralButton("삭제") { _, _ ->
                deleteImage()
            }
            .setPositiveButton("갤러리") { _, _ ->
                startGallery()
            }
            .setNegativeButton("카메라") { _, _ ->
                startCamera()
            }
            .create()
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
        when {
            ContextCompat.checkSelfPermission(
                this,
                readPermission
            ) == PackageManager.PERMISSION_GRANTED -> {
                getPhoto()
            } // 외부저장소 접근 권한이 잘 부여되어있을 때, 갤러리에서 사진을 선택
            shouldShowRequestPermissionRationale(readPermission) -> {
                showPermissionPopup()
            } // 이전에 앱이 권한을 요청하고 사용자가 요청을 거부한 경우 교육용 팝업을 띄움
            else -> {
                requestPermissions(arrayOf(readPermission), 1000)
            } // 권한 요청을 위한 팝업을 띄움
        }
    }

    private fun startCamera() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                cameraPermission
            ) == PackageManager.PERMISSION_GRANTED -> {
                activateCamera()
            } // 카메라 사용 권한이 잘 부여되어있을 때, 카메라 실행
            shouldShowRequestPermissionRationale(cameraPermission) -> {
                showCameraPermissionPopup()
            } // 이전에 앱이 권한을 요청하고 사용자가 요청을 거부한 경우 교육용 팝업을 띄움
            else -> {
                requestPermissions(arrayOf(cameraPermission), 1001)
            } // 권한 요청을 위한 팝업을 띄움
        }
    }

    private fun showPermissionPopup() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다.")
            .setMessage("더 무비앱에서 사진을 불러오기 위해 권한이 필요합니다.")
            .setPositiveButton("허용") { _, _ ->
                requestPermissions(arrayOf(readPermission), 1000)
            }
            .setNegativeButton("차단") { _, _ -> }
            .create()
            .show()
    }

    private fun showCameraPermissionPopup() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다.")
            .setMessage("더 무비앱에서 사진을 촬영하기 위해 권한이 필요합니다.")
            .setPositiveButton("허용") { _, _ ->
                requestPermissions(arrayOf(cameraPermission), 1001)
            }
            .setNegativeButton("차단") { _, _ -> }
            .create()
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getPhoto() // 권한이 부여됨
                } else {
                    Toast.makeText(this, "권한을 거부하셨습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    activateCamera() // 권한이 부여됨
                } else {
                    Toast.makeText(this, "권한을 거부하셨습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    } // requestPermissions()의 처리 결과를 반환해주는 메소드

    private fun getPhoto() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        activityResult.launch(intent)
    } // Storage Access Framework(SAF)의 기능을 이용해서 콘텐츠를 가져올 수 있는 안드로이드의 내장 엑티비티를 실행해주는 메소드

    private fun activateCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val dir = externalCacheDir
            val file = File.createTempFile("review_photo_", ".jpg", dir)
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            activityResultCamera.launch(intent)
            photoFile = file
        }
    } // 카메라를 실행시키고 촬영한 사진을 앱의 캐시 저장소에 저장해주는 메소드

    private val activityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    selectedImageUri = result?.data?.data
                    if (selectedImageUri != null) {
                        Glide.with(this).load(selectedImageUri).centerCrop()
                            .into(binding.imageView) // 사진을 올바르게 돌려서 imageView에 보여주기 위해 Glide 라이브러리 사용
                    } else {
                        Toast.makeText(this, "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {
                    Toast.makeText(this, "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val activityResultCamera =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    selectedImageUri = photoFile.toUri()
                    if (selectedImageUri != null) {
                        Glide.with(this).load(photoFile).centerCrop()
                            .into(binding.imageView) // 사진을 올바르게 돌려서 imageView에 보여주기 위해 Glide 라이브러리 사용
                    } else {
                        Toast.makeText(this, "사진을 가져오지 못했습니다.22", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {
                    Toast.makeText(this, "사진을 가져오지 못했습니다.11", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun initContentEditText() {
        binding.contentEditText.setOnFocusChangeListener { view, b ->
            if (!b) {
                Utils.hideKeyboard(this, view)
            }
        }
    }

    private fun initSaveButton() {
        binding.button.setOnClickListener {
            dataChanged()
        }
    }

    private fun dataChanged() {
        viewModel = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(ReviewViewModel::class.java)

        val currentReviewTitle = binding.titleEditText.text.toString()
        val currentReviewContent = binding.contentEditText.text.toString()
        val currentRating = binding.reviewRatingBar.rating.toDouble() * 2

        lifecycleScope.launch(Dispatchers.Main) {
            showProgress()
            if (fileName != "" || selectedImageUri != null) {
                deletePhotoStorage()
            } // 사용자가 이미지를 수정하면 이전에 서버에 저장되어 있던 이미지는 삭제
            uploadPhotoToStorage() // 이미지를 Firebase Storage에 업로드

            if (reviewType.equals("Edit")) {
                if (currentReviewTitle.isNotBlank() && currentReviewContent.isNotBlank()) {
                    val updateReview = Review(
                        currentReviewTitle,
                        imageUri,
                        currentReviewContent,
                        Utils.getCurrentDate(),
                        currentRating,
                        fileName
                    )
                    updateReview.id = reviewId

                    viewModel.updateReview(updateReview)

                    updateFirebaseRealtimeDB(
                        currentReviewTitle,
                        currentReviewContent,
                        currentRating
                    )

                    Toast.makeText(applicationContext, "감상문이 수정되었습니다.", Toast.LENGTH_LONG).show()
                    returnToReviewMain()
                } else {
                    Toast.makeText(applicationContext, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_LONG)
                        .show()
                }
            } else {
                if (currentReviewTitle.isNotBlank() && currentReviewContent.isNotBlank()) {
                    val updateReview = Review(
                        currentReviewTitle,
                        imageUri,
                        currentReviewContent,
                        Utils.getCurrentDate(),
                        currentRating,
                        fileName
                    )

                    viewModel.insertTransaction(updateReview)
                    viewModel.maxId
                        .observe(this@ReviewDetailActivity, Observer { maxId ->
                            maxId?.let {
                                reviewId = it
                                updateFirebaseRealtimeDB(
                                    currentReviewTitle,
                                    currentReviewContent,
                                    currentRating
                                )
                                Toast.makeText(
                                    applicationContext,
                                    "감상문이 등록되었습니다.",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                                returnToReviewMain()
                            }
                        })
                } else {
                    Toast.makeText(applicationContext, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_LONG)
                        .show()
                }
            }
            hideProgress()
        }
    }

    private suspend fun deletePhotoStorage() {
        try {
            storage.reference.child("review/photo").child(fileName).delete().await()
            fileName = ""
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun uploadPhotoToStorage() {
        if (selectedImageUri == null) {
            return
        } // 사용자가 이미지를 감상문에 등록하지 않았다면 메소드 종료

        fileName = userId.substring(
            0,
            10
        ) + "${System.currentTimeMillis()}.png" // Storage에 저장될 File의 이름을 지정
        try {
            imageUri = storage.reference.child("review/photo").child(fileName)
                .putFile(selectedImageUri!!).await().storage.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } // 이미지를 Firebase Storage의 지정된 경로에 업로드해주고 해당 이미지를 가져올 수 있는 Url을 반환해오는 메소드

    private fun updateFirebaseRealtimeDB(
        currentReviewTitle: String,
        currentReviewContent: String,
        currentRating: Double
    ) {
        reviewDB =
            Firebase.database.reference.child("users").child(userId).child("reviews")
                .child(reviewId.toString())
        val review = mutableMapOf<String, Any>()
        review["id"] = reviewId
        review["image"] = imageUri
        review["title"] = currentReviewTitle
        review["content"] = currentReviewContent
        review["time"] = Utils.getCurrentDate()
        review["rating"] = currentRating
        review["storageFileName"] = fileName
        try {
            reviewDB.updateChildren(review)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } // 서버에 감상문 정보를 저장(Firebase Realtime Database)해주는 메소드

    private fun returnToReviewMain() {
        val intent = Intent(applicationContext, ReviewMainActivity::class.java)
        intent.putExtra("type", "Edit")
        startActivity(intent)
        this.finish()
    }

    private fun showProgress() {
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) // 화면 터치 막기
        binding.progressBar.isVisible = true
    }

    private fun hideProgress() {
        binding.progressBar.isVisible = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) // 화면 터치 풀기
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val intent = Intent(this, ReviewMainActivity::class.java)
            startActivity(intent)
            this.finish()
        }
        return false
    }
}