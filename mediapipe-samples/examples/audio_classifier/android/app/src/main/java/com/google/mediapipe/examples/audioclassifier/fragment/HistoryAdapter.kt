package com.google.mediapipe.examples.audioclassifier.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.mediapipe.examples.audioclassifier.AlertInfo
import com.google.mediapipe.examples.audioclassifier.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var historyList: List<AlertInfo> = emptyList()
    private val timeFormatter = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

    fun setHistory(newList: List<AlertInfo>) {
        historyList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(historyList[position])
    }

    override fun getItemCount(): Int = historyList.size

    inner class ViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(info: AlertInfo) {
            binding.historyLabel.text = info.label
            binding.historyTime.text = timeFormatter.format(Date(info.timestamp))
            binding.historyConfidence.text = String.format("%.1f%%", info.confidence * 100)
            
            // Set accent color based on tier
            binding.root.setStrokeColor(
                android.content.res.ColorStateList.valueOf(
                    binding.root.context.getColor(info.tier.colorRes)
                )
            )
        }
    }
}
