package com.codealphas.themovie.presentation.review

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
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
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.load
import coil3.request.error
import com.codealphas.themovie.core.android.ui.applySystemBarInsets
import com.codealphas.themovie.core.android.ui.setupAppBar
import com.codealphas.themovie.domain.review.ReviewError
import com.codealphas.themovie.domain.review.ReviewPhoto
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.databinding.ActivityReviewDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class ReviewDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewDetailBinding
    private val viewModel: ReviewEditViewModel by viewModels()
    private val cameraPermission: String by lazy { Manifest.permission.CAMERA }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()

        binding = ActivityReviewDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAppBar(binding.toolbar, getString(R.string.review_edit_appbar_title))

        initTitleEditText()
        initImageView()
        initContentEditText()
        initRatingBar()
        initSaveButton()
        observeState()
        observeEvents()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        // 입력을 상태에 반영한 값과 같은데 다시 넣으면 커서가 처음으로 돌아가므로, 다를 때만 입력란에 반영
                        if (binding.titleEditText.text.toString() != state.title) {
                            binding.titleEditText.setText(state.title)
                        }
                        if (binding.contentEditText.text.toString() != state.content) {
                            binding.contentEditText.setText(state.content)
                        }
                        if (binding.reviewRatingBar.rating != state.starRating) {
                            binding.reviewRatingBar.rating = state.starRating
                        }
                        binding.button.setText(
                            if (state.isEditing) R.string.review_edit_update else R.string.review_edit_create,
                        )
                        setProgressVisible(state.isSaving)
                    }
                }
                launch {
                    // 입력 중에도 상태가 계속 바뀌므로, 사진이 바뀔 때만 다시 로드
                    viewModel.uiState
                        .map { it.photo to it.savedImageUrl }
                        .distinctUntilChanged()
                        .collect { (photo, savedImageUrl) -> showPhoto(photo, savedImageUrl) }
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is ReviewEditEvent.Saved -> {
                            showToast(if (event.isNew) R.string.review_edit_created else R.string.review_edit_updated)
                            finish()
                        }
                        ReviewEditEvent.InputRequired -> showToast(R.string.review_edit_input_required)
                        is ReviewEditEvent.SaveFailed ->
                            showToast(
                                when (event.error) {
                                    ReviewError.PhotoUploadFailed -> R.string.review_edit_upload_failed
                                    ReviewError.Unknown -> R.string.review_edit_save_failed
                                },
                            )
                        ReviewEditEvent.LoadFailed -> {
                            showToast(R.string.review_edit_load_failed)
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun showPhoto(
        photo: ReviewPhoto,
        savedImageUrl: String,
    ) {
        val imageData =
            when (photo) {
                is ReviewPhoto.New -> photo.uri
                is ReviewPhoto.Unchanged if savedImageUrl.isNotEmpty() -> savedImageUrl
                else -> null
            }

        if (imageData == null) {
            binding.imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            // 사진을 지운 뒤 늦게 끝난 이전 로드가 기본 이미지를 덮지 않도록 기본 이미지도 Coil로 로드해 이전 로드 대체
            binding.imageView.load(R.drawable.set_image)
            return
        }

        binding.imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.imageView.load(imageData) {
            error(R.drawable.set_image)
            listener(onError = { _, _ -> binding.imageView.scaleType = ImageView.ScaleType.FIT_CENTER })
        }
    }

    private fun initTitleEditText() {
        binding.titleEditText.doOnTextChanged { text, _, _, _ -> viewModel.onTitleChange(text.toString()) }

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
                viewModel.onPhotoRemoved()
            }.setPositiveButton(getString(R.string.review_edit_photo_gallery)) { _, _ ->
                startGallery()
            }.setNegativeButton(getString(R.string.review_edit_photo_camera)) { _, _ ->
                startCamera()
            }.create()
            .show()
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

    private fun activateCamera() {
        val file = File.createTempFile("review_photo_", ".jpg", externalCacheDir)
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        viewModel.onCameraStarted(file.toUri().toString())
        // 카메라 앱이 없는 기기에서는 실행 시 예외가 나므로, 앱 종료 대신 실패 안내 처리
        try {
            cameraLauncher.launch(uri)
        } catch (ignored: ActivityNotFoundException) {
            showToast(R.string.review_edit_photo_failed)
        }
    } // 카메라를 실행시키고 촬영한 사진을 앱의 캐시 저장소에 저장해주는 메소드

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                activateCamera()
            } else {
                showToast(R.string.review_edit_permission_denied)
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // 선택 없이 닫은 경우는 실패가 아니므로 안내 없이 무시
            if (uri != null) {
                viewModel.onPhotoSelected(uri.toString())
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSaved ->
            viewModel.onCameraResult(isSaved)
            if (!isSaved) {
                showToast(R.string.review_edit_photo_failed)
            }
        }

    private fun initContentEditText() {
        binding.contentEditText.doOnTextChanged { text, _, _, _ -> viewModel.onContentChange(text.toString()) }

        binding.contentEditText.setOnFocusChangeListener { view, b ->
            if (!b) {
                hideKeyboard(this, view)
            }
        }
    }

    private fun initRatingBar() {
        binding.reviewRatingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            // 상태를 반영하려고 코드로 바꾼 별점은 이미 상태에 있는 값이므로, 사용자가 바꾼 경우만 전달
            if (fromUser) {
                viewModel.onStarRatingChange(rating)
            }
        }
    }

    private fun initSaveButton() {
        binding.button.setOnClickListener {
            viewModel.save()
        }
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(applicationContext, getString(messageResId), Toast.LENGTH_LONG).show()
    }

    private fun setProgressVisible(isVisible: Boolean) {
        if (isVisible) {
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) // 화면 터치 막기
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) // 화면 터치 풀기
        }
        binding.progressBar.isVisible = isVisible
    }

    private fun hideKeyboard(
        context: Context,
        view: View,
    ) {
        val inputMethodManager =
            context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
