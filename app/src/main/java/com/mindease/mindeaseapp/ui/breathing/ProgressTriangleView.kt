package com.mindease.mindeaseapp.ui.breathing

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.mindease.mindeaseapp.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom View untuk 4-7-8 Breathing dengan progress bar yang mengikuti sisi triangle.
 *
 * Progress bar berjalan mengikuti fase:
 * - Phase 0 (Inhale 4s): Left side (bottom-left → top)
 * - Phase 1 (Hold 7s): Right side (top → bottom-right)
 * - Phase 2 (Exhale 8s): Bottom side (bottom-right → bottom-left)
 */
class ProgressTriangleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint objects
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(8f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(8f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // State
    private var currentPhase = 0 // 0=left, 1=right, 2=bottom
    private var progress = 0f // 0.0 - 1.0
    private var animatedProgress = 0f // For fade animation

    // Colors
    private var primaryColor: Int = 0
    private var borderColor: Int = 0

    // Triangle points
    private val topPoint = PointF()
    private val bottomLeftPoint = PointF()
    private val bottomRightPoint = PointF()

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

        val padding = borderPaint.strokeWidth
        val centerX = w / 2f

        // Calculate equilateral triangle points dengan margin lebih besar
        val triangleHeight = h - padding * 2
        val triangleSide = triangleHeight * 2f / (kotlin.math.sqrt(3f))

        // FIX: Tambah margin internal agar progress tidak keluar jalur
        val internalMargin = cornerRadius

        topPoint.set(centerX, padding + internalMargin)
        bottomLeftPoint.set(centerX - triangleSide / 2 + internalMargin, h - padding - internalMargin)
        bottomRightPoint.set(centerX + triangleSide / 2 - internalMargin, h - padding - internalMargin)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw border (inactive background)
        drawTriangle(canvas, borderPaint)

        // Draw progress bar based on current phase
        if (animatedProgress > 0f) {
            drawProgressForPhase(canvas, currentPhase, animatedProgress)
        }
    }

    private fun drawTriangle(canvas: Canvas, paint: Paint) {
        val path = Path()

        // Draw simple triangle dengan rounded effect
        path.moveTo(bottomLeftPoint.x, bottomLeftPoint.y)
        path.lineTo(topPoint.x, topPoint.y)
        path.lineTo(bottomRightPoint.x, bottomRightPoint.y)
        path.close()

        canvas.drawPath(path, paint)
    }

    /**
     * Draw progress bar for specific phase
     */
    private fun drawProgressForPhase(canvas: Canvas, phase: Int, prog: Float) {
        val path = Path()

        when (phase) {
            0 -> {
                // Left side: bottom-left → top (Inhale 4s)
                val startX = bottomLeftPoint.x
                val startY = bottomLeftPoint.y
                val endX = topPoint.x
                val endY = topPoint.y

                val currentX = startX + (endX - startX) * prog
                val currentY = startY + (endY - startY) * prog

                path.moveTo(startX, startY)
                path.lineTo(currentX, currentY)
            }
            1 -> {
                // Right side: top → bottom-right (Hold 7s)
                val startX = topPoint.x
                val startY = topPoint.y
                val endX = bottomRightPoint.x
                val endY = bottomRightPoint.y

                val currentX = startX + (endX - startX) * prog
                val currentY = startY + (endY - startY) * prog

                path.moveTo(startX, startY)
                path.lineTo(currentX, currentY)
            }
            2 -> {
                // Bottom side: bottom-right → bottom-left (Exhale 8s)
                val startX = bottomRightPoint.x
                val startY = bottomRightPoint.y
                val endX = bottomLeftPoint.x
                val endY = bottomLeftPoint.y

                val currentX = startX + (endX - startX) * prog
                val currentY = startY + (endY - startY) * prog

                path.moveTo(startX, startY)
                path.lineTo(currentX, currentY)
            }
        }

        canvas.drawPath(path, progressPaint)
    }

    /**
     * Set current phase (0-2) with fade animation
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