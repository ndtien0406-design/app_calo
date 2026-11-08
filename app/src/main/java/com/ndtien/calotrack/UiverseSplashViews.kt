package com.ndtien.calotrack

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hiệu ứng vòng xoay kép ngược chiều + sóng xung (Sonar Pulse) lấy cảm hứng từ Uiverse.io (/loaders),
 * bao quanh logo chính trên màn hình khởi động.
 */
class UiverseOrbitView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val primaryColor = ContextCompat.getColor(context, R.color.proto_primary)
    private val accentColor = ContextCompat.getColor(context, R.color.chart_under_label)

    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density
        color = primaryColor
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = ColorUtils.setAlphaComponent(primaryColor, 36)
    }

    private val outerArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * density
        strokeCap = Paint.Cap.ROUND
        color = primaryColor
    }

    private val innerArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
        strokeCap = Paint.Cap.ROUND
        color = ColorUtils.setAlphaComponent(accentColor, 190)
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = accentColor
    }

    private val dotHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ColorUtils.setAlphaComponent(accentColor, 70)
    }

    private val outerRect = RectF()
    private val innerRect = RectF()
    private var phase = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxR = (width.coerceAtMost(height) / 2f) - 4f * density
        val outerR = maxR * 0.76f
        val innerR = maxR * 0.63f

        // 1. Hai vòng sóng xung (Sonar Ripple) lan tỏa tuần tự từ tâm ra ngoài
        for (i in 0..1) {
            val p = (phase + i * 0.5f) % 1f
            val r = innerR + (maxR - innerR) * p
            val alpha = ((1f - p) * (1f - p) * 95).toInt().coerceIn(0, 255)
            pulsePaint.alpha = alpha
            canvas.drawCircle(cx, cy, r, pulsePaint)
        }

        // 2. Đường ray mờ định hình quỹ đạo
        canvas.drawCircle(cx, cy, outerR, trackPaint)

        // 3. Cung xoay ngoài (cùng chiều kim đồng hồ) + hạt sáng dẫn đầu
        outerRect.set(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
        val outerStart = phase * 360f - 90f
        val outerSweep = 115f
        canvas.drawArc(outerRect, outerStart, outerSweep, false, outerArcPaint)

        val headAngleRad = Math.toRadians((outerStart + outerSweep).toDouble())
        val hx = cx + outerR * cos(headAngleRad).toFloat()
        val hy = cy + outerR * sin(headAngleRad).toFloat()
        canvas.drawCircle(hx, hy, 5.5f * density, dotHaloPaint)
        canvas.drawCircle(hx, hy, 2.8f * density, dotPaint)

        // 4. Cung xoay trong (ngược chiều kim đồng hồ, tạo chiều sâu 2 lớp kiểu Uiverse)
        innerRect.set(cx - innerR, cy - innerR, cx + innerR, cy + innerR)
        val innerStart = 270f - phase * 720f
        canvas.drawArc(innerRect, innerStart, 80f, false, innerArcPaint)
    }
}

/**
 * Thanh 5 cột sóng nhịp điệu (Uiverse Wave Bars Loader) hiển thị trạng thái đang tải mượt mà.
 */
class UiverseWaveBarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val topColor = ContextCompat.getColor(context, R.color.chart_under_label)
    private val bottomColor = ContextCompat.getColor(context, R.color.proto_primary)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val rect = RectF()
    private var phase = 0f

    private val animator = ValueAnimator.ofFloat(0f, (Math.PI * 2).toFloat()).apply {
        duration = 1100L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (h > 0) {
            barPaint.shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                topColor, bottomColor, Shader.TileMode.CLAMP
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val barCount = 5
        val barWidth = 5.5f * density
        val gap = 7f * density
        val totalWidth = barCount * barWidth + (barCount - 1) * gap
        val startX = (width - totalWidth) / 2f
        val cy = height / 2f
        val maxBarHeight = height * 0.88f
        val minBarHeight = height * 0.26f
        val corner = barWidth / 2f

        for (i in 0 until barCount) {
            val wave = (sin((phase - i * 0.62f).toDouble()).toFloat() + 1f) * 0.5f
            val h = minBarHeight + (maxBarHeight - minBarHeight) * wave
            val left = startX + i * (barWidth + gap)
            rect.set(left, cy - h / 2f, left + barWidth, cy + h / 2f)
            barPaint.alpha = (155 + (wave * 100).toInt()).coerceIn(0, 255)
            canvas.drawRoundRect(rect, corner, corner, barPaint)
        }
    }
}

