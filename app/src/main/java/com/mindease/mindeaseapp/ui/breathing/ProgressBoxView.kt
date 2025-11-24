package com.mindease.mindeaseapp.ui.breathing

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.mindease.mindeaseapp.R

/**
 * Custom View untuk Box Breathing dengan progress bar yang mengikuti sisi box.
 *
 * Progress bar berjalan mengikuti fase:
 * - Phase 0 (Inhale): Top side (left → right)
 * - Phase 1 (Hold In): Right side (top → bottom)
 * - Phase 2 (Exhale): Bottom side (right → left)
 * - Phase 3 (Hold Out): Left side (bottom → top)
 */
class ProgressBoxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint objects
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(8f)
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(8f)
        strokeCap = Paint.Cap.ROUND
    }

    // State
    private var currentPhase = 0 // 0=top, 1=right, 2=bottom, 3=left
    private var progress = 0f // 0.0 - 1.0
    private var animatedProgress = 0f // For fade animation

    // Colors
    private var primaryColor: Int = 0
    private var borderColor: Int = 0

    // Box dimensions
    private val boxRect = RectF()
    private val cornerRadius = dpToPx(8f)

    // Animation
    private var fadeAnimator: ValueAnimator? = null

    init {
        updateColors()
    }

    /**
     * Update colors based on current theme
     */
    fun updateColors() {
        primaryColor = ContextCompat.getColor(context, R.color.mindease_primary)

        // Get border color based on theme
        val typedValue = android.util.TypedValue()
        context.theme.resolveAttribute(
            com.google.android.material.R.attr.colorOnSurface,
            typedValue,
            true
        )
        borderColor = typedValue.data

        borderPaint.color = adjustAlpha(borderColor, 0.3f)
        progressPaint.color = primaryColor

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val padding = borderPaint.strokeWidth / 2
        boxRect.set(
            padding,
            padding,
            w - padding,
            h - padding
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw border (inactive background)
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, borderPaint)

        // Draw progress bar based on current phase
        if (animatedProgress > 0f) {
            drawProgressForPhase(canvas, currentPhase, animatedProgress)
        }
    }

    /**
     * Draw progress bar for specific phase
     */
    private fun drawProgressForPhase(canvas: Canvas, phase: Int, prog: Float) {
        val path = Path()
        val left = boxRect.left
        val top = boxRect.top
        val right = boxRect.right
        val bottom = boxRect.bottom

        when (phase) {
            0 -> {
                // Top side: left → right (Inhale)
                val endX = left + (right - left) * prog
                path.moveTo(left + cornerRadius, top)
                path.lineTo(endX.coerceAtMost(right - cornerRadius), top)
            }
            1 -> {
                // Right side: top → bottom (Hold In)
                val endY = top + (bottom - top) * prog
                path.moveTo(right, top + cornerRadius)
                path.lineTo(right, endY.coerceAtMost(bottom - cornerRadius))
            }
            2 -> {
                // Bottom side: right → left (Exhale)
                val startX = right - (right - left) * prog
                path.moveTo(right - cornerRadius, bottom)
                path.lineTo(startX.coerceAtLeast(left + cornerRadius), bottom)
            }
            3 -> {
                // Left side: bottom → top (Hold Out)
                val startY = bottom - (bottom - top) * prog
                path.moveTo(left, bottom - cornerRadius)
                path.lineTo(left, startY.coerceAtLeast(top + cornerRadius))
            }
        }

        canvas.drawPath(path, progressPaint)
    }

    /**
     * Set current phase (0-3) with fade animation
     */
    fun setPhase(phase: Int) {
        if (currentPhase != phase) {
            currentPhase = phase

            // Quick fade out animation
            fadeAnimator?.cancel()
            fadeAnimator = ValueAnimator.ofFloat(animatedProgress, 0f).apply {
                duration = 200
                interpolator = LinearInterpolator()
                addUpdateListener {
                    animatedProgress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }

    /**
     * Set progress (0.0 - 1.0) for current phase
     */
    fun setProgress(prog: Float) {
        progress = prog.coerceIn(0f, 1f)

        // Cancel fade if new progress is set
        if (fadeAnimator?.isRunning == true) {
            fadeAnimator?.cancel()
        }

        animatedProgress = progress
        invalidate()
    }

    /**
     * Reset progress to 0
     */
    fun resetProgress() {
        progress = 0f
        animatedProgress = 0f
        invalidate()
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt()
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}