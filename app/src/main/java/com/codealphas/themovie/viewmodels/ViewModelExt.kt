package com.codealphas.themovie.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// HTTP 오류가 코루틴 밖으로 나가면 프로세스가 종료된다
fun ViewModel.launchRequest(block: suspend () -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            block()
        } catch (_: Exception) {
        }
    }
}
