/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mediapipe.examples.audioclassifier

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private var _threshold: Float = AudioClassifierHelper.DISPLAY_THRESHOLD
    private var _maxResults: Int = AudioClassifierHelper.DEFAULT_NUM_OF_RESULTS
    private var _overlapPosition: Int = 2

    val currentThreshold: Float get() = _threshold
    val currentMaxResults: Int get() = _maxResults
    val currentOverlapPosition: Int get() = _overlapPosition

    private val _history = MutableLiveData<List<AlertInfo>>(emptyList())
    val history: LiveData<List<AlertInfo>> = _history

    private val _isDiagnosticActive = MutableLiveData(false)
    val isDiagnosticActive: LiveData<Boolean> = _isDiagnosticActive

    private val _queuedDiagnosticEvent = MutableLiveData<String?>(null)
    val queuedDiagnosticEvent: LiveData<String?> = _queuedDiagnosticEvent

    fun setThreshold(threshold: Float) {
        _threshold = threshold
    }

    fun setMaxResults(maxResults: Int) {
        _maxResults = maxResults
    }

    fun setOverlap(position: Int) {
        _overlapPosition = position
    }

    fun setDiagnosticActive(active: Boolean) {
        _isDiagnosticActive.value = active
    }

    fun queueDiagnosticTrigger(label: String) {
        _queuedDiagnosticEvent.value = label
        // Reset after read
        _queuedDiagnosticEvent.value = null
    }

    fun addHistory(info: AlertInfo) {
        val currentList = _history.value ?: emptyList()
        val newList = listOf(info) + currentList
        _history.value = newList.take(10)
    }
}
