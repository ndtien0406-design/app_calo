package com.ndtien.calotrack

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Đồng hồ vòng tròn calo (Uiverse Radial Progress Gauge) hiển thị trên thẻ Hero Trang chủ:
 * - Vòng nền mảnh + cung tiến trình bo tròn 2 đầu (Cap.ROUND) + hào quang mảnh
 * - Nhãn phần trăm tabular-nums ở chính giữa
 */
class UiverseCalorieRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dp = resources.displayMetrics.density
    private val sp = resources.displayMetrics.scaledDensity
    private val arcBounds = RectF()

    private var shownPercent: Float = 0f
    private var ringAnimator: ValueAnimator? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f * dp
        strokeCap = Paint.Cap.ROUND
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 9.5f * dp
        strokeCap = Paint.Cap.ROUND
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f * dp
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 11.5f * sp
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        fontFeatureSettings = "tnum"
    }

    fun setProgress(
        percent: Int,
        ringColor: Int,
        trackColor: Int,
        textColor: Int,
        animate: Boolean = true
    ) {
        trackPaint.color = trackColor
        arcPaint.color = ringColor
        glowPaint.color = ColorUtils.setAlphaComponent(ringColor, 0x38)
        textPaint.color = textColor

        val target = percent.coerceAtLeast(0).toFloat()
        ringAnimator?.cancel()
        if (!animate) {
            shownPercent = target
            invalidate()
            return
        }
        val startVal = shownPercent
        ringAnimator = ValueAnimator.ofFloat(startVal, target).apply {
            duration = 550L
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { anim ->
                shownPercent = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        ringAnimator?.cancel()
        ringAnimator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val pad = 5.5f * dp
        val size = min(w, h) - pad * 2f
        val left = (w - size) / 2f
        val top = (h - size) / 2f
        arcBounds.set(left, top, left + size, top + size)

        canvas.drawOval(arcBounds, trackPaint)

        val sweep = (shownPercent.coerceIn(0f, 100f) / 100f) * 360f
        if (sweep > 0.5f) {
            canvas.drawArc(arcBounds, -90f, sweep, false, glowPaint)
            canvas.drawArc(arcBounds, -90f, sweep, false, arcPaint)
        }

        val label = "${shownPercent.roundToInt().coerceAtMost(999)}%"
        val fm = textPaint.fontMetrics
        val baselineY = h / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(label, w / 2f, baselineY, textPaint)
    }
}
