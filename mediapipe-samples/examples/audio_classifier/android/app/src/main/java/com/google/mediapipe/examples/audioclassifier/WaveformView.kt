package com.google.mediapipe.examples.audioclassifier

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.sin

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val numBars = 32
    private val amplitudes = FloatArray(numBars) { 0.1f }
    private var accentColor = ContextCompat.getColor(context, R.color.waveform_idle)
    
    private var phase = 0f

    fun updateAmplitude(newAmplitude: Float) {
        // Shift existing amplitudes and add new one
        for (i in 0 until numBars - 1) {
            amplitudes[i] = amplitudes[i + 1]
        }
        amplitudes[numBars - 1] = newAmplitude.coerceIn(0.1f, 1.0f)
        postInvalidateOnAnimation()
    }

    fun setAccentColor(color: Int) {
        accentColor = color
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val barWidth = (width / numBars) * 0.6f
        val spacing = (width / numBars) * 0.4f
        
        paint.color = accentColor
        
        phase += 0.1f
        
        for (i in 0 until numBars) {
            // Add a "breathing" effect if amplitude is low
            val breathe = if (amplitudes[i] <= 0.15f) (sin(phase + i * 0.5).toFloat() + 1f) * 0.05f else 0f
            val barHeight = (amplitudes[i] + breathe) * height
            
            val left = i * (barWidth + spacing) + spacing / 2
            val top = (height - barHeight) / 2
            val right = left + barWidth
            val bottom = top + barHeight
            
            canvas.drawRoundRect(left, top, right, bottom, barWidth / 2, barWidth / 2, paint)
        }
        
        // Continuous animation for breathing
        if (amplitudes.all { it <= 0.15f }) {
            postInvalidateOnAnimation()
        }
    }
}
