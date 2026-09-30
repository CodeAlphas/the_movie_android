package com.codealphas.themovie.movie

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.codealphas.themovie.R
import com.codealphas.themovie.auth.LoginActivity
import com.codealphas.themovie.core.android.ui.applySystemBarInsets
import com.codealphas.themovie.core.android.ui.setupAppBar
import com.codealphas.themovie.databinding.ActivityMainBinding
import com.codealphas.themovie.notification.NotificationPermissionState
import com.codealphas.themovie.notification.NotificationPromptAction
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private companion object {
        const val TAB_COUNT = 3
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val tabTitleResIds =
        intArrayOf(
            R.string.movie_list_tab_popular,
            R.string.movie_list_tab_top_rated,
            R.string.movie_list_tab_search,
        )
    private val tabIcons =
        arrayListOf(
            R.drawable.ic_baseline_movie_popular_24,
            R.drawable.ic_baseline_top_movies_24,
            R.drawable.ic_baseline_search_24,
        )

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Snackbar.make(binding.root, R.string.notification_permission_denied, Snackbar.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.isSignedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        applySystemBarInsets()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAppBar(binding.toolbar, getString(R.string.movie_list_appbar_popular), showBack = false)
        initViewPager()
        initTabLayout()
        linkViewPagerAndTabLayout()
        observeLogout()
        observeNotificationPrompt()
        // 회전처럼 화면이 다시 만들어질 때마다 확인하면 나중에를 누르기 전 안내가 또 뜨므로, 처음 만들 때만 자동 안내 확인
        if (savedInstanceState == null) {
            viewModel.onMainEntered(notificationPermissionState())
        }
    }

    private fun observeLogout() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.logoutCompleted.collect {
                    startActivity(Intent(applicationContext, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun observeNotificationPrompt() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notificationPrompt.collect { action ->
                    when (action) {
                        NotificationPromptAction.SHOW_RATIONALE -> showNotificationPrompt()
                        NotificationPromptAction.OPEN_SETTINGS -> openNotificationSettings()
                        NotificationPromptAction.NONE -> Unit
                    }
                }
            }
        }
    }

    private fun notificationPermissionState(): NotificationPermissionState {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        return NotificationPermissionState(
            sdkInt = Build.VERSION.SDK_INT,
            isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED,
            shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission),
        )
    }

    private fun showNotificationPrompt() {
        AlertDialog
            .Builder(this)
            .setTitle(R.string.notification_prompt_title)
            .setMessage(R.string.notification_prompt_message)
            .setPositiveButton(R.string.notification_prompt_allow) { _, _ ->
                viewModel.onNotificationPromptAccepted()
                requestNotificationPermission()
            }.setNegativeButton(R.string.notification_prompt_later) { _, _ ->
                viewModel.onNotificationPromptDeclined()
            }
            // 뒤로 가기나 바깥 터치로 닫아도 나중에와 같은 뜻이므로, 다음 실행에 자동 안내가 다시 뜨지 않도록 기록
            .setOnCancelListener { viewModel.onNotificationPromptDeclined() }
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openNotificationSettings() {
        // 앱 알림 설정 화면은 Android 8.0(API 26)부터 있으므로, minSdk 24 기기에서는 앱 정보 화면으로 이동
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            }
        startActivity(intent)
    }

    private fun initViewPager() {
        binding.viewPager.adapter = FragmentViewPagerAdapter(this, TAB_COUNT) // 뷰페이저에 Adapter 장착
    }

    private fun initTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val position = tab!!.position
                    binding.viewPager.currentItem = position

                    when (position) {
                        0 -> supportActionBar?.title = getString(R.string.movie_list_appbar_popular)
                        1 -> supportActionBar?.title = getString(R.string.movie_list_top_rated_subtitle)
                        else -> supportActionBar?.title = getString(R.string.movie_list_appbar_search)
                    } // 탭 클릭시 해당 탭에 맞게 앱바(액션바)의 텍스트 변경
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

                override fun onTabReselected(tab: TabLayout.Tab?) = Unit
            },
        ) // 탭 레이아웃 설정
    }

    private fun linkViewPagerAndTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = getString(tabTitleResIds[position])
            tab.icon = getDrawable(tabIcons[position])
        }.attach() // 탭 레이아웃과 뷰페이저 연결
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.application_menu, menu)
        menuInflater.inflate(R.menu.main_notification_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.logout_action -> {
                viewModel.logout()
                true
            }
            R.id.notification_settings_action -> {
                viewModel.onNotificationSettingsClicked(notificationPermissionState())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}
