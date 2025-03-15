package com.wavky.memorycard.app.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

abstract class BaseViewModel : ViewModel() {

  fun <T> Flow<T>.stateIn(initialValue: T) = stateIn(
    viewModelScope,
    started = SharingStarted.WhileSubscribed(),
    initialValue = initialValue
  )
}
