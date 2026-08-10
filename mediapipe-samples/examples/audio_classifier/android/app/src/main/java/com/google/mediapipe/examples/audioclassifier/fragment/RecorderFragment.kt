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

package com.google.mediapipe.examples.audioclassifier.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.mediapipe.examples.audioclassifier.*
import com.google.mediapipe.examples.audioclassifier.databinding.FragmentRecorderBinding
import com.google.mediapipe.tasks.audio.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RecorderFragment : Fragment(), AudioClassifierHelper.ClassifierListener {
    private var _fragmentBinding: FragmentRecorderBinding? = null
    private val fragmentRecorderBinding get() = _fragmentBinding!!
    private lateinit var audioClassifierHelper: AudioClassifierHelper
    private lateinit var historyAdapter: HistoryAdapter
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var alertManager: AlertManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentAlertTier: AlertTier = AlertTier.LOW
    private var isAlertLocked = false
    private val handler = Handler(Looper.getMainLooper())
    private var emergencyCountdownTimer: Handler? = null

    private val resetAlertRunnable = Runnable {
        resetAlertUi()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentBinding =
            FragmentRecorderBinding.inflate(inflater, container, false)
        return fragmentRecorderBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentRecorderBinding.bottomSheetLayout.rlInferenceTime.visibility =
            View.GONE
        backgroundExecutor = Executors.newSingleThreadExecutor()
        alertManager = AlertManager(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // init the history recyclerview
        historyAdapter = HistoryAdapter()
        with(fragmentRecorderBinding.historyRecyclerView) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        viewModel.history.observe(viewLifecycleOwner) {
            historyAdapter.setHistory(it)
        }

        fragmentRecorderBinding.btnCancelEmergency.setOnClickListener {
            cancelEmergencyFlow()
        }

        viewModel.queuedDiagnosticEvent.observe(viewLifecycleOwner) { label ->
            if (label != null) {
                executeInternalTestFlow(label)
            }
        }

        backgroundExecutor.execute {
            audioClassifierHelper =
                AudioClassifierHelper(
                    context = requireContext(),
                    classificationThreshold = viewModel.currentThreshold,
                    overlap = viewModel.currentOverlapPosition,
                    numOfResults = viewModel.currentMaxResults,
                    runningMode = RunningMode.AUDIO_STREAM,
                    listener = this,
                    amplitudeListener = { amplitude ->
                        activity?.runOnUiThread {
                            if (_fragmentBinding != null) {
                                fragmentRecorderBinding.waveformVisualizer.updateAmplitude(amplitude)
                            }
                        }
                    }
                )
            activity?.runOnUiThread {
                initBottomSheetControls()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(
                requireActivity(),
                R.id.fragment_container
            )
                .navigate(R.id.action_audio_to_permissions)
        }
        backgroundExecutor.execute {
            if (audioClassifierHelper.isClosed()) {
                audioClassifierHelper.initClassifier()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // save audio classifier settings
        viewModel.apply {
            setThreshold(audioClassifierHelper.classificationThreshold)
            setMaxResults(audioClassifierHelper.numOfResults)
            setOverlap(audioClassifierHelper.overlap)
        }

        backgroundExecutor.execute {
            if (::audioClassifierHelper.isInitialized) {
                audioClassifierHelper.stopAudioClassification()
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _fragmentBinding = null
        // Shut down our background executor
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    private fun initBottomSheetControls() {

        // Allow the user to change the amount of overlap used in classification. More overlap
        // can lead to more accurate resolves in classification.
        fragmentRecorderBinding.bottomSheetLayout.spinnerOverlap.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    audioClassifierHelper.overlap = position
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    // no op
                }
            }

        // Allow the user to change the max number of results returned by the audio classifier.
        // Currently allows between 1 and 5 results, but can be edited here.
        fragmentRecorderBinding.bottomSheetLayout.resultsMinus.setOnClickListener {
            if (audioClassifierHelper.numOfResults > 1) {
                audioClassifierHelper.numOfResults--
                updateControlsUi()
            }
        }

        fragmentRecorderBinding.bottomSheetLayout.resultsPlus.setOnClickListener {
            if (audioClassifierHelper.numOfResults < 5) {
                audioClassifierHelper.numOfResults++
                updateControlsUi()
            }
        }

        // Allow the user to change the confidence threshold required for the classifier to return
        // a result. Increments in steps of 10%.
        fragmentRecorderBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (audioClassifierHelper.classificationThreshold >= 0.2) {
                audioClassifierHelper.classificationThreshold -= 0.1f
                updateControlsUi()
            }
        }

        fragmentRecorderBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (audioClassifierHelper.classificationThreshold <= 0.8) {
                audioClassifierHelper.classificationThreshold += 0.1f
                updateControlsUi()
            }
        }

        fragmentRecorderBinding.bottomSheetLayout.spinnerOverlap.setSelection(
            viewModel.currentOverlapPosition,
            false
        )

        fragmentRecorderBinding.bottomSheetLayout.thresholdValue.text =
            viewModel.currentThreshold.toString()
        fragmentRecorderBinding.bottomSheetLayout.resultsValue.text =
            viewModel.currentMaxResults.toString()
    }

    // Update the values displayed in the bottom sheet. Reset classifier.
    private fun updateControlsUi() {
        fragmentRecorderBinding.bottomSheetLayout.resultsValue.text =
            audioClassifierHelper.numOfResults.toString()
        fragmentRecorderBinding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", audioClassifierHelper.classificationThreshold)

        backgroundExecutor.execute {
            audioClassifierHelper.stopAudioClassification()
            audioClassifierHelper.initClassifier()
        }
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResult(resultBundle: AudioClassifierHelper.ResultBundle) {
        // Suppress real detections if Diagnostic mode is active
        if (viewModel.isDiagnosticActive.value == true) return
        
        activity?.runOnUiThread {
            if (_fragmentBinding != null) {
                resultBundle.results[0].classificationResults().first()
                    .classifications()?.get(0)?.categories()?.let { categories ->
                        val topCategory = categories.maxByOrNull { it.score() }
                        if (topCategory != null && topCategory.score() >= audioClassifierHelper.classificationThreshold) {
                            processDetection(topCategory.categoryName(), topCategory.score())
                        }
                    }
            }
        }
    }

    private fun processDetection(label: String, confidence: Float) {
        val tier = alertManager.getTierForLabel(label)
        
        // Priority Override: Critical sounds bypass the hold timer
        if (tier.priority > currentAlertTier.priority || !isAlertLocked) {
            updateAlertUi(label, confidence, tier)
        }
    }

    private fun resetAlertUi() {
        isAlertLocked = false
        currentAlertTier = AlertTier.LOW
        fragmentRecorderBinding.alertCard.visibility = View.INVISIBLE
        fragmentRecorderBinding.tvStatus.visibility = View.VISIBLE
        fragmentRecorderBinding.waveformVisualizer.setAccentColor(
            ContextCompat.getColor(requireContext(), R.color.waveform_idle)
        )
        fragmentRecorderBinding.emergencyOverlay.visibility = View.GONE
    }

    private fun triggerEmergencyFlow() {
        fragmentRecorderBinding.emergencyOverlay.visibility = View.VISIBLE
        var secondsLeft = 5
        
        emergencyCountdownTimer = Handler(Looper.getMainLooper())
        val countdownRunnable = object : Runnable {
            override fun run() {
                if (secondsLeft > 0) {
                    fragmentRecorderBinding.emergencyTimerText.text = "Sending SMS in ${secondsLeft}s..."
                    fragmentRecorderBinding.emergencyProgress.progress = (5 - secondsLeft) * 20
                    secondsLeft--
                    emergencyCountdownTimer?.postDelayed(this, 1000)
                } else {
                    sendEmergencySms()
                    resetAlertUi()
                }
            }
        }
        emergencyCountdownTimer?.post(countdownRunnable)
    }

    private fun cancelEmergencyFlow() {
        emergencyCountdownTimer?.removeCallbacksAndMessages(null)
        resetAlertUi()
    }

    private fun sendEmergencySms() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Emergency SMS failed: Missing Permissions", Toast.LENGTH_LONG).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val mapsUrl = if (location != null) {
                "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
            } else {
                "Location unavailable"
            }

            val message = "EcoSense EMERGENCY! Fire + Scream detected. Live Location: $mapsUrl"
            val phoneNumber = getString(R.string.config_dispatch_endpoint)

            try {
                val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    requireContext().getSystemService(SmsManager::class.java)
                } else {
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Toast.makeText(requireContext(), "Emergency SMS Sent to $phoneNumber", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "SMS Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun executeInternalTestFlow(label: String) {
        // 5-second invisible timer for presentation
        handler.postDelayed({
            val randomConfidence = (74 + (Math.random() * 14)).toFloat() / 100f
            val tier = alertManager.getTierForLabel(label)
            
            // For diagnostic events, we might want a longer hold (5 seconds as per plan)
            updateAlertUi(label, randomConfidence, tier, holdDuration = 5000L)
        }, 5000)
    }

    private fun updateAlertUi(label: String, confidence: Float, tier: AlertTier, holdDuration: Long = 2000L) {
        // SMS/GPS Overlay ONLY for Fusion tier AND only when Diagnostic mode is ON
        if (tier == AlertTier.FUSION && viewModel.isDiagnosticActive.value == true) {
            triggerEmergencyFlow()
            return
        }
        
        handler.removeCallbacks(resetAlertRunnable)
        
        currentAlertTier = tier
        isAlertLocked = true
        
        fragmentRecorderBinding.alertCard.visibility = View.VISIBLE
        fragmentRecorderBinding.alertTitle.text = label
        fragmentRecorderBinding.alertConfidence.text = String.format("%.1f%%", confidence * 100)
        
        fragmentRecorderBinding.alertCard.setStrokeColor(
            android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), tier.colorRes)
            )
        )

        fragmentRecorderBinding.waveformVisualizer.setAccentColor(
            ContextCompat.getColor(requireContext(), tier.colorRes)
        )
        
        fragmentRecorderBinding.tvStatus.visibility = View.GONE
        
        // Add to history
        viewModel.addHistory(AlertInfo(label, tier, confidence))
        
        // Trigger vibration
        alertManager.triggerVibration(tier)
        
        // Start hold timer
        handler.postDelayed(resetAlertRunnable, holdDuration)
    }

}
