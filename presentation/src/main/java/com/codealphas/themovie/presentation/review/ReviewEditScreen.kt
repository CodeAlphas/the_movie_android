package com.codealphas.themovie.presentation.review

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.core.android.ui.BlockingProgressBox
import com.codealphas.themovie.core.android.ui.TheMovieTopAppBar
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.core.android.ui.showToast
import com.codealphas.themovie.domain.review.ReviewError
import com.codealphas.themovie.presentation.R
import java.io.File

private val FieldShape = RoundedCornerShape(5.dp)
private val FieldBorderWidth = 1.dp
private val StarSize = 36.dp
private const val PHOTO_WEIGHT = 0.4f
private const val CONTENT_WEIGHT = 0.6f
private const val PREVIEW_RATING = 7.0

@Composable
fun ReviewEditScreen(
    onNavigateUp: () -> Unit,
    viewModel: ReviewEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = checkNotNull(LocalActivity.current)
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnNavigateUp by rememberUpdatedState(onNavigateUp)
    var showCameraRationale by rememberSaveable { mutableStateOf(false) }
    // 저장 완료와 불러오기 실패는 안내 직후 화면을 닫으므로, 화면이 사라진 뒤에도 안내가 남도록 앱 Context로 Toast 표시
    val appContext = context.applicationContext

    // 화면이 멈춘 동안 보낸 안내를 돌아와서 받도록, 수집은 STARTED 동안만 하고 채널에 남은 effect는 다시 시작할 때 처리
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is ReviewEditEffect.Saved -> {
                        appContext.showToast(
                            if (effect.isNew) R.string.review_edit_created else R.string.review_edit_updated,
                            Toast.LENGTH_LONG,
                        )
                        currentOnNavigateUp()
                    }
                    ReviewEditEffect.InputRequired ->
                        context.showToast(
                            R.string.review_edit_input_required,
                            Toast.LENGTH_LONG,
                        )
                    is ReviewEditEffect.SaveFailed ->
                        context.showToast(
                            saveErrorMessage(effect.error),
                            Toast.LENGTH_LONG,
                        )
                    ReviewEditEffect.LoadFailed -> {
                        appContext.showToast(R.string.review_edit_load_failed, Toast.LENGTH_LONG)
                        currentOnNavigateUp()
                    }
                }
            }
        }
    }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // 선택 없이 닫은 경우는 실패가 아니므로 안내 없이 무시
            if (uri != null) viewModel.onIntent(ReviewEditIntent.PhotoSelected(uri.toString()))
        }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isSaved ->
            viewModel.onIntent(ReviewEditIntent.CameraFinished(isSaved))
            if (!isSaved) context.showToast(R.string.review_edit_photo_failed, Toast.LENGTH_LONG)
        }
    val launchCamera = {
        val file = File.createTempFile("review_photo_", ".jpg", context.externalCacheDir)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        viewModel.onIntent(ReviewEditIntent.CameraStarted(file.toUri().toString()))
        // 카메라 앱이 없는 기기에서는 실행 시 예외가 나므로, 앱 종료 대신 실패 안내 처리
        try {
            cameraLauncher.launch(uri)
        } catch (ignored: ActivityNotFoundException) {
            context.showToast(R.string.review_edit_photo_failed, Toast.LENGTH_LONG)
        }
    }
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                context.showToast(
                    R.string.review_edit_permission_denied,
                    Toast.LENGTH_LONG,
                )
            }
        }

    ReviewEditContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp,
        onGalleryClick = {
            // 권한이 필요 없는 Photo Picker로 갤러리 사진 선택 요청
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onCameraClick = {
            val permission = Manifest.permission.CAMERA
            when {
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED ->
                    launchCamera()
                // 한 번 거부한 뒤 바로 다시 요청하면 이유를 모른 채 같은 창을 보게 되므로, 권한이 필요한 이유 안내 창 표시
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) ->
                    showCameraRationale = true
                else -> cameraPermissionLauncher.launch(permission)
            }
        },
    )

    if (showCameraRationale) {
        CameraPermissionDialog(
            onAllow = {
                showCameraRationale = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onDeny = { showCameraRationale = false },
        )
    }
}

@StringRes
private fun saveErrorMessage(error: ReviewError): Int =
    when (error) {
        ReviewError.PhotoUploadFailed -> R.string.review_edit_upload_failed
        ReviewError.Unknown -> R.string.review_edit_save_failed
    }

@Composable
internal fun ReviewEditContent(
    state: ReviewEditUiState,
    onIntent: (ReviewEditIntent) -> Unit,
    onNavigateUp: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var showPhotoDialog by rememberSaveable { mutableStateOf(false) }

    // 저장 중에 화면을 닫으면 viewModelScope가 취소되어 사진 업로드 중인 감상문이 안내 없이 저장되지 않으므로,
    // 앱바까지 덮도록 Scaffold 전체를 감싸 저장이 끝날 때까지 터치와 뒤로 가기 차단
    BlockingProgressBox(isBlocking = state.isSaving, modifier = Modifier.fillMaxSize()) {
        // 앱바 색이 라이트에서 어둡고 다크에서 밝아 상태 표시줄 뒤까지 칠하면 같은 계열 색의 시계와 배터리 아이콘이 묻히므로,
        // 상태 표시줄 뒤에는 창 배경이 보이도록 앱바를 상태 표시줄 높이만큼 내려 배치
        Scaffold(
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            topBar = {
                TheMovieTopAppBar(
                    title = stringResource(R.string.review_edit_appbar_title),
                    onNavigateUp = onNavigateUp,
                )
            },
            contentWindowInsets = WindowInsets.safeDrawing,
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        // Compose는 입력란 밖을 눌러도 포커스를 유지해 키보드가 남으므로, 입력란 밖을 누르면 키보드가 내려가도록 포커스 해제
                        .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
                        .padding(Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                ReviewTextField(
                    value = state.title,
                    onValueChange = { onIntent(ReviewEditIntent.TitleChanged(it)) },
                    hint = R.string.review_edit_title_hint,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ReviewPhotoBox(
                    image = state.shownImage,
                    onClick = { showPhotoDialog = true },
                    modifier = Modifier.fillMaxWidth().weight(PHOTO_WEIGHT),
                )
                ReviewTextField(
                    value = state.content,
                    onValueChange = { onIntent(ReviewEditIntent.ContentChanged(it)) },
                    hint = R.string.review_edit_content_hint,
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().weight(CONTENT_WEIGHT),
                )
                ReviewEditBottomBar(state = state, onIntent = onIntent)
            }
        }
    }

    if (showPhotoDialog) {
        PhotoSourceDialog(
            onDismiss = { showPhotoDialog = false },
            onRemove = {
                showPhotoDialog = false
                onIntent(ReviewEditIntent.PhotoRemoved)
            },
            onGallery = {
                showPhotoDialog = false
                onGalleryClick()
            },
            onCamera = {
                showPhotoDialog = false
                onCameraClick()
            },
        )
    }
}

@Composable
private fun ReviewTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes hint: Int,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.border(FieldBorderWidth, MaterialTheme.colorScheme.outline, FieldShape),
        placeholder = { Text(text = stringResource(hint)) },
        // 한 줄 칸은 엔터로 줄을 바꿀 수 없으므로, 완료 키를 누르면 키보드가 내려가도록 포커스 해제
        keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        singleLine = singleLine,
        shape = FieldShape,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
    )
}

@Composable
private fun ReviewPhotoBox(
    image: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val defaultImage = painterResource(R.drawable.set_image)
    val painter =
        rememberAsyncImagePainter(
            model = image,
            placeholder = defaultImage,
            error = defaultImage,
            fallback = defaultImage,
        )
    val painterState by painter.state.collectAsStateWithLifecycle()
    Box(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, FieldShape)
                .border(FieldBorderWidth, MaterialTheme.colorScheme.outline, FieldShape)
                .clickable(onClick = rememberThrottledClick(onClick = onClick))
                .padding(Spacing.small),
    ) {
        // 기본 이미지는 카메라 아이콘과 문구가 잘리지 않도록 칸 안에 맞추고, 불러온 사진만 칸을 채우도록 잘라서 표시
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.review_edit_photo_description),
            modifier = Modifier.fillMaxSize(),
            contentScale =
                if (painterState is AsyncImagePainter.State.Success) ContentScale.Crop else ContentScale.Fit,
        )
    }
}

@Composable
private fun ReviewEditBottomBar(
    state: ReviewEditUiState,
    onIntent: (ReviewEditIntent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, FieldShape)
                .border(FieldBorderWidth, MaterialTheme.colorScheme.outline, FieldShape)
                .padding(Spacing.small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RatingStarsPicker(
            starRating = state.starRating,
            onStarRatingChange = { onIntent(ReviewEditIntent.StarRatingChanged(it)) },
            starSize = StarSize,
        )
        Button(
            onClick = rememberThrottledClick { onIntent(ReviewEditIntent.SaveClicked) },
            modifier = Modifier.weight(1f),
            enabled = !state.isSaving,
            shape = FieldShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
        ) {
            Text(
                text =
                    stringResource(
                        if (state.isEditing) R.string.review_edit_update else R.string.review_edit_create,
                    ),
            )
        }
    }
}

@Composable
private fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.review_edit_photo_dialog_title)) },
        text = { Text(text = stringResource(R.string.review_edit_photo_dialog_message)) },
        confirmButton = {
            Row {
                TextButton(onClick = onRemove) { Text(text = stringResource(R.string.review_edit_photo_delete)) }
                TextButton(onClick = onCamera) { Text(text = stringResource(R.string.review_edit_photo_camera)) }
                TextButton(onClick = onGallery) { Text(text = stringResource(R.string.review_edit_photo_gallery)) }
            }
        },
    )
}

@Composable
private fun CameraPermissionDialog(
    onAllow: () -> Unit,
    onDeny: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDeny,
        title = { Text(text = stringResource(R.string.review_edit_permission_title)) },
        text = { Text(text = stringResource(R.string.review_edit_permission_camera)) },
        confirmButton = {
            TextButton(onClick = onAllow) { Text(text = stringResource(R.string.review_edit_permission_allow)) }
        },
        dismissButton = {
            TextButton(onClick = onDeny) { Text(text = stringResource(R.string.review_edit_permission_deny)) }
        },
    )
}

@Preview(name = "라이트")
@Preview(name = "다크", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReviewEditContentPreview() {
    ReviewEditContentPreview(
        state = ReviewEditUiState(title = "Title", content = "Content", rating = PREVIEW_RATING),
    )
}

@Preview(name = "수정 중 저장")
@Composable
private fun ReviewEditContentSavingPreview() {
    ReviewEditContentPreview(
        state = ReviewEditUiState(title = "Title", content = "Content", isEditing = true, isSaving = true),
    )
}

@Composable
private fun ReviewEditContentPreview(state: ReviewEditUiState) {
    TheMovieTheme {
        ReviewEditContent(
            state = state,
            onIntent = {},
            onNavigateUp = {},
            onGalleryClick = {},
            onCameraClick = {},
        )
    }
}
