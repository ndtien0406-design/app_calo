package com.ndtien.calotrack

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.math.abs

/**
 * Nút hành động nổi (+) chuẩn Uiverse.io x OpenDesign x CT999 (alodev-soft-tech):
 * - F1: Lõi Ngọc Lục Bảo 3D vát sáng đỉnh 1.35dp + Vòng hào quang thở Sonar 2.4s.
 * - F2: Vệt ánh sáng pha lê quét chéo 35 độ (Diagonal Shimmer Sweep).
 * - F3: Xoay biểu tượng (+) hai chiều 0 <-> 135 độ theo trạng thái đóng/mở BottomSheet.
 * - F4: Tự động thu gọn & né thông minh khi cuộn danh sách (Scroll-Aware Auto-Dodge).
 * - F5: Phản hồi chạm từ tâm (Magnetic Press 0.90x -> Spring Overshoot + Sóng lan tỏa).
 */
class UiverseOrbFabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dp = resources.displayMetrics.density
    private val isNightMode: Boolean
        get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private val coreRect = RectF()
    private val rimRect = RectF()
    private val shadowLayerRect = RectF()
    private val sonarRect = RectF()
    private val waveRect = RectF()
    private val corePath = Path()
    private val shimmerMatrix = Matrix()

    // F1: Hào quang nền + vòng Sonar thở
    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val sonarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val tapWavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#6EE7B7")
    }

    // F1: Thân nút 3D Emerald Core + Top Specular Rim Light + Bóng đổ đa lớp mềm
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * dp
    }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * dp
    }

    // Vòng kính mờ nội tâm quanh dấu +
    private val innerRingFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1CFFFFFF")
    }
    private val innerRingStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * dp
        color = Color.parseColor("#48FFFFFF")
    }

    // F2: Vệt sáng quét chéo 35 độ
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private var shimmerShader: LinearGradient? = null

    // F3: Biểu tượng dấu + có bóng chiều sâu
    private val iconShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 3f * dp
        color = Color.parseColor("#42032214")
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.8f * dp
        color = Color.parseColor("#F7FFF9")
    }

    // Trạng thái hoạt ảnh
    private var sonarPhase = 0f
    private var shimmerProgress = -1f
    private var tapWaveProgress = -1f
    private var iconRotationDeg = 0f
    private var pressScale = 1f
    private var isDodged = false

    private var sonarAnimator: ValueAnimator? = null
    private var shimmerAnimator: ValueAnimator? = null
    private var tapWaveAnimator: ValueAnimator? = null
    private var iconSpinAnimator: ValueAnimator? = null
    private var pressAnimator: ValueAnimator? = null

    private val periodicShimmerRunnable = object : Runnable {
        override fun run() {
            if (isAttachedToWindow && visibility == VISIBLE && !isDodged) {
                triggerShimmerSweep()
            }
            postDelayed(this, 5500L)
        }
    }

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSize = (72f * dp).toInt()
        val w = resolveSize(defaultSize, widthMeasureSpec)
        val h = resolveSize(defaultSize, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        val cx = w / 2f
        val cy = h / 2f
        val maxR = minOf(w, h) / 2f
        val night = isNightMode

        auraPaint.shader = RadialGradient(
            cx,
            cy + 1.5f * dp,
            maxR,
            if (night) {
                intArrayOf(
                    Color.parseColor("#7552B788"),
                    Color.parseColor("#2E52B788"),
                    Color.TRANSPARENT
                )
            } else {
                intArrayOf(
                    Color.parseColor("#4D40916C"),
                    Color.parseColor("#1E52B788"),
                    Color.TRANSPARENT
                )
            },
            floatArrayOf(0.32f, 0.70f, 1f),
            Shader.TileMode.CLAMP
        )

        sonarPaint.color = if (night) Color.parseColor("#74F2B5") else Color.parseColor("#38A86D")
        outerBorderPaint.color = if (night) Color.parseColor("#4286EFAC") else Color.parseColor("#24083A20")

        // Lõi nút 52dp x 52dp nằm chính giữa khung 72dp (đệm 10dp mỗi cạnh cho hào quang và bóng)
        val coreHalf = 26f * dp
        coreRect.set(cx - coreHalf, cy - coreHalf, cx + coreHalf, cy + coreHalf)
        val rimInset = 0.85f * dp
        rimRect.set(
            coreRect.left + rimInset,
            coreRect.top + rimInset,
            coreRect.right - rimInset,
            coreRect.bottom - rimInset
        )

        bodyPaint.shader = LinearGradient(
            coreRect.left,
            coreRect.top,
            coreRect.right,
            coreRect.bottom,
            if (night) {
                intArrayOf(
                    Color.parseColor("#8CEAA9"),
                    Color.parseColor("#43B579"),
                    Color.parseColor("#1D7A49")
                )
            } else {
                intArrayOf(
                    Color.parseColor("#7CE39D"),
                    Color.parseColor("#38A86D"),
                    Color.parseColor("#1B7042")
                )
            },
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )

        rimPaint.shader = LinearGradient(
            0f,
            coreRect.top,
            0f,
            coreRect.bottom,
            intArrayOf(
                Color.parseColor("#EBFFFFFF"),
                Color.parseColor("#5298F5C2"),
                Color.parseColor("#18FFFFFF")
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )

        val bandWidth = coreRect.width() * 0.55f
        shimmerShader = LinearGradient(
            0f,
            0f,
            bandWidth,
            bandWidth * 0.7f,
            intArrayOf(
                Color.TRANSPARENT,
                Color.parseColor("#26FFFFFF"),
                Color.parseColor("#7AFFFFFF"),
                Color.parseColor("#26FFFFFF"),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        shimmerPaint.shader = shimmerShader
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startSonarLoop()
        removeCallbacks(periodicShimmerRunnable)
        postDelayed(periodicShimmerRunnable, 600L)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(periodicShimmerRunnable)
        sonarAnimator?.cancel()
        shimmerAnimator?.cancel()
        tapWaveAnimator?.cancel()
        iconSpinAnimator?.cancel()
        pressAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun startSonarLoop() {
        sonarAnimator?.cancel()
        sonarAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2400L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener {
                sonarPhase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /** Kích hoạt vệt ánh sáng pha lê quét chéo 35 độ qua mặt nút. */
    fun triggerShimmerSweep() {
        shimmerAnimator?.cancel()
        shimmerAnimator = ValueAnimator.ofFloat(-0.35f, 1.35f).apply {
            duration = 720L
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                shimmerProgress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    shimmerProgress = -1f
                    invalidate()
                }
            })
            start()
        }
    }

    /** Kích hoạt vòng sóng lan tỏa khi nhấn nút. */
    private fun triggerTapWave() {
        tapWaveAnimator?.cancel()
        tapWaveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 460L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                tapWaveProgress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tapWaveProgress = -1f
                    invalidate()
                }
            })
            start()
        }
    }

    /**
     * F3: Xoay biểu tượng dấu (+) 0 <-> 135 độ theo trạng thái mở/đóng của BottomSheet.
     */
    fun setSheetOpen(open: Boolean) {
        val targetDeg = if (open) 135f else 0f
        if (abs(iconRotationDeg - targetDeg) < 0.5f && iconSpinAnimator?.isRunning != true) {
            return
        }
        iconSpinAnimator?.cancel()
        iconSpinAnimator = ValueAnimator.ofFloat(iconRotationDeg, targetDeg).apply {
            duration = 320L
            interpolator = OvershootInterpolator(2.1f)
            addUpdateListener {
                iconRotationDeg = it.animatedValue as Float
                invalidate()
            }
            start()
        }
        if (!open) {
            triggerShimmerSweep()
        }
    }

    /**
     * F4: Thu gọn và né sang góc khi người dùng cuộn xuống danh sách để không che số kcal & nút xóa.
     */
    fun setDodged(dodged: Boolean) {
        if (isDodged == dodged) return
        isDodged = dodged
        animate()
            .scaleX(if (dodged) 0.78f else 1f)
            .scaleY(if (dodged) 0.78f else 1f)
            .translationY(if (dodged) 8f * dp else 0f)
            .translationX(if (dodged) 6f * dp else 0f)
            .alpha(if (dodged) 0.76f else 1f)
            .setDuration(220L)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    /**
     * Liên kết đồng bộ nút (+) với RecyclerView (tự né khi cuộn + đệm đáy 112dp)
     * và FragmentManager (tự xoay dấu + sang x khi mở BottomSheet và xoay về + khi đóng).
     */
    fun bindToScreen(recyclerView: RecyclerView, fragmentManager: FragmentManager) {
        val minBottomPadding = (112f * dp).toInt()
        if (recyclerView.paddingBottom < minBottomPadding) {
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                recyclerView.paddingTop,
                recyclerView.paddingRight,
                minBottomPadding
            )
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 5) {
                    setDodged(true)
                } else if (dy < -5 || !rv.canScrollVertically(-1)) {
                    setDodged(false)
                }
            }
        })

        fragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                    if (f is BottomSheetDialogFragment) {
                        setDodged(false)
                        setSheetOpen(true)
                    }
                }

                override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
                    if (f is BottomSheetDialogFragment) {
                        setSheetOpen(false)
                    }
                }
            },
            false
        )
    }

    private fun animatePressScale(target: Float, durationMs: Long, overshoot: Boolean) {
        pressAnimator?.cancel()
        pressAnimator = ValueAnimator.ofFloat(pressScale, target).apply {
            duration = durationMs
            interpolator = if (overshoot) OvershootInterpolator(2.6f) else FastOutSlowInInterpolator()
            addUpdateListener {
                pressScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                setDodged(false)
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                animatePressScale(0.90f, 90L, overshoot = false)
                triggerShimmerSweep()
                return true
            }
            MotionEvent.ACTION_UP -> {
                animatePressScale(1f, 280L, overshoot = true)
                triggerTapWave()
                setSheetOpen(true)
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                animatePressScale(1f, 200L, overshoot = false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val cornerRadius = 17f * dp
        val night = isNightMode

        // 1. Vẽ hào quang nền (Ambient Radial Glow)
        canvas.drawCircle(cx, cy + 1f * dp, minOf(width, height) / 2f, auraPaint)

        // 2. Vẽ vòng hào quang thở Sonar (Uiverse Sonar Pulse Halo) nằm gọn trong biên 10dp
        val maxExpand = 7.0f * dp
        val expand = sonarPhase * maxExpand
        sonarRect.set(
            coreRect.left - expand,
            coreRect.top - expand,
            coreRect.right + expand,
            coreRect.bottom + expand
        )
        val baseSonarAlpha = if (night) 155f else 120f
        val sonarAlpha = ((1f - sonarPhase) * (1f - sonarPhase) * baseSonarAlpha).toInt().coerceIn(0, 255)
        sonarPaint.alpha = sonarAlpha
        sonarPaint.strokeWidth = (2.0f - 1.0f * sonarPhase) * dp
        val sonarRadius = cornerRadius + expand * 0.85f
        canvas.drawRoundRect(sonarRect, sonarRadius, sonarRadius, sonarPaint)

        // 3. Vẽ sóng lan tỏa khi chạm (Tap Ripple Wave)
        if (tapWaveProgress in 0f..1f) {
            val waveExpand = tapWaveProgress * 8.0f * dp
            waveRect.set(
                coreRect.left - waveExpand,
                coreRect.top - waveExpand,
                coreRect.right + waveExpand,
                coreRect.bottom + waveExpand
            )
            tapWavePaint.alpha = ((1f - tapWaveProgress) * 210f).toInt().coerceIn(0, 255)
            tapWavePaint.strokeWidth = (2.5f - 1.5f * tapWaveProgress) * dp
            val waveR = cornerRadius + waveExpand
            canvas.drawRoundRect(waveRect, waveR, waveR, tapWavePaint)
        }

        // Phép biến đổi co giãn tâm nút khi nhấn (Magnetic Press)
        canvas.save()
        canvas.scale(pressScale, pressScale, cx, cy)

        // 4. Bóng đổ đa lớp tán xạ mềm (Multi-Layer Feathered Soft Shadow, không viền cứng)
        val shadowBaseColor = if (night) 0x020E08 else 0x0D3822
        for (layer in 1..4) {
            val spread = layer * 0.9f * dp
            val dy = layer * 0.95f * dp
            val alpha = (5 - layer) * (if (night) 14 else 9)
            shadowPaint.color = (alpha shl 24) or shadowBaseColor
            shadowLayerRect.set(
                coreRect.left - spread * 0.4f,
                coreRect.top + dy - spread * 0.2f,
                coreRect.right + spread * 0.4f,
                coreRect.bottom + dy + spread * 0.5f
            )
            val sr = cornerRadius + spread * 0.4f
            canvas.drawRoundRect(shadowLayerRect, sr, sr, shadowPaint)
        }

        // 5. Thân nút Ngọc Lục Bảo 3D (Gradient 135 độ)
        canvas.drawRoundRect(coreRect, cornerRadius, cornerRadius, bodyPaint)

        // 6. Vệt ánh sáng pha lê quét chéo 35 độ (Diagonal Shimmer Sweep)
        if (shimmerProgress > -0.4f && shimmerShader != null) {
            corePath.reset()
            corePath.addRoundRect(coreRect, cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(corePath)
            val travel = coreRect.width() * 1.9f
            val tx = coreRect.left - coreRect.width() * 0.45f + shimmerProgress * travel
            shimmerMatrix.reset()
            shimmerMatrix.postRotate(25f)
            shimmerMatrix.postTranslate(tx, coreRect.top - 8f * dp)
            shimmerShader?.setLocalMatrix(shimmerMatrix)
            canvas.drawRect(coreRect, shimmerPaint)
            canvas.restore()
        }

        // 7. Viền vát sáng đỉnh 1.4dp (Top Specular Rim Light) & viền ngoài
        val rimRadius = cornerRadius - 0.85f * dp
        canvas.drawRoundRect(rimRect, rimRadius, rimRadius, rimPaint)
        canvas.drawRoundRect(coreRect, cornerRadius, cornerRadius, outerBorderPaint)

        // 8. Vòng tròn kính mờ nội tâm
        val innerOrbRadius = 15f * dp
        canvas.drawCircle(cx, cy, innerOrbRadius, innerRingFillPaint)
        canvas.drawCircle(cx, cy, innerOrbRadius, innerRingStrokePaint)

        // 9. Biểu tượng dấu (+) xoay lò xo hai chiều (0 <-> 135 độ)
        val halfArm = 8.2f * dp
        canvas.save()
        canvas.rotate(iconRotationDeg, cx, cy)

        // Bóng đổ của dấu +
        val shadowOffset = 1.1f * dp
        canvas.drawLine(cx - halfArm, cy + shadowOffset, cx + halfArm, cy + shadowOffset, iconShadowPaint)
        canvas.drawLine(cx, cy - halfArm + shadowOffset, cx, cy + halfArm + shadowOffset, iconShadowPaint)

        // Nét chính của dấu +
        canvas.drawLine(cx - halfArm, cy, cx + halfArm, cy, iconPaint)
        canvas.drawLine(cx, cy - halfArm, cx, cy + halfArm, iconPaint)
        canvas.restore()

        canvas.restore()
    }
}
