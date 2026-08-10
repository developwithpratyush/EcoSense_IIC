package com.google.mediapipe.examples.audioclassifier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.mediapipe.examples.audioclassifier.databinding.LayoutInternalParamsBinding

class DiagnosticSystemPanel : BottomSheetDialogFragment() {

    private var _binding: LayoutInternalParamsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutInternalParamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchDiagnostic.isChecked = viewModel.isDiagnosticActive.value ?: false
        binding.switchDiagnostic.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDiagnosticActive(isChecked)
        }

        binding.actionParam0.setOnClickListener { trigger("Fire Alarm") }
        binding.actionParam1.setOnClickListener { trigger("Emergency Siren") }
        binding.actionParam2.setOnClickListener { trigger("Fire + Scream") }
        binding.actionParam3.setOnClickListener { trigger("Doorbell") }
        binding.actionParam4.setOnClickListener { trigger("Personal Sound") }
        binding.actionParam5.setOnClickListener { trigger("Baby Crying") }
    }

    private fun trigger(label: String) {
        viewModel.queueDiagnosticTrigger(label)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
