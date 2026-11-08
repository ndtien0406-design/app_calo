package com.ndtien.calotrack

import android.content.Context
import android.graphics.Canvas
import android.graphics. Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

/**
 * Nền trang Ambient Radial Mesh Glow + Micro Dot-Matrix (24dp)
 * Kết hợp Uiverse.io (/patterns) và chuẩn bộ não CT999 (imagegen-frontend-mobile Rule 17 + phong-cach-ceo R9).
 * Chỉ tính toán Shader khi kích thước đổi (onSizeChanged), không chạy vòng lặp để giữ 0% tải CPU khi đứng yên.
 */
class UiverseAmbientBgView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val surfaceColor = ContextCompat.getColor(context, R.color.proto_surface)
    private val glowTopColor = ContextCompat.getColor(context, R.color.ambient_glow_top)
    private val glowBottomColor = ContextCompat.getColor(context, R.color.ambient_glow_bottom)
    private val dotColor = ContextCompat.getColor(context, R.color.ambient_dot_color)

    private val topGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bottomGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = dotColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        val topR = (w * 0.82f).coerceAtLeast(1f)
        topGlowPaint.shader = RadialGradient(
            w * 0.16f,
            h * 0.06f,
            topR,
            intArrayOf(glowTopColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        val botR = (w * 0.76f).coerceAtLeast(1f)
        bottomGlowPaint.shader = RadialGradient(
            w * 0.86f,
            h * 0.88f,
            botR,
            intArrayOf(glowBottomColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // 1. Nền gốc Tonal Surface
        canvas.drawColor(surfaceColor)

        // 2. Hai quầng sáng khuếch tán (Radial Mesh Glow) tạo độ sâu không gian
        canvas.drawRect(0f, 0f, w, h, topGlowPaint)
        canvas.drawRect(0f, 0f, w, h, bottomGlowPaint)

        // 3. Lưới chấm vi mô (Micro Dot-Matrix 24dp) kiểu Uiverse /patterns
        val step = 24f * density
        val dotRadius = 0.95f * density
        var y = step * 0.5f
        while (y < h) {
            var x = step * 0.5f
            while (x < w) {
                canvas.drawCircle(x, y, dotRadius, dotPaint)
                x += step
            }
            y += step
        }
    }
}
