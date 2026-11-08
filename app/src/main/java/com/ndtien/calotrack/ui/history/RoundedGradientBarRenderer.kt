package com.ndtien.calotrack.ui.history

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlin.math.min

/**
 * Bộ vẽ cột biểu đồ bo tròn đỉnh 10dp + chuyển sắc dọc 180 độ + viền sáng đỉnh 1.5dp
 * theo chuẩn Uiverse x CT999 alodev-soft-tech.
 */
class RoundedGradientBarRenderer(
    chart: BarDataProvider,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler,
    private val cornerRadiusPx: Float
) : BarChartRenderer(chart, animator, viewPortHandler) {

    private val barRect = RectF()
    private val barPath = Path()
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = cornerRadiusPx * 0.14f
    }

    override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
        val trans = mChart.getTransformer(dataSet.axisDependency)
        val phaseX = mAnimator.phaseX
        val phaseY = mAnimator.phaseY

        if (mBarBuffers == null || index >= mBarBuffers.size) return
        val buffer = mBarBuffers[index] ?: return
        buffer.setPhases(phaseX, phaseY)
        buffer.setDataSet(index)
        buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
        buffer.setBarWidth(mChart.barData.barWidth)
        buffer.feed(dataSet)
        trans.pointValuesToPixel(buffer.buffer)

        val isSingleColor = dataSet.colors.size == 1
        var j = 0
        while (j < buffer.size()) {
            val left = buffer.buffer[j]
            val top = buffer.buffer[j + 1]
            val right = buffer.buffer[j + 2]
            val bottom = buffer.buffer[j + 3]

            if (!mViewPortHandler.isInBoundsLeft(right)) {
                j += 4
                continue
            }
            if (!mViewPortHandler.isInBoundsRight(left)) break

            val barHeight = bottom - top
            if (barHeight > 1.5f) {
                val baseColor = if (isSingleColor) dataSet.color else dataSet.getColor(j / 4)
                val topColor = ColorUtils.blendARGB(baseColor, Color.WHITE, 0.24f)
                val bottomColor = ColorUtils.blendARGB(baseColor, Color.BLACK, 0.18f)

                mRenderPaint.shader = LinearGradient(
                    0f, top, 0f, bottom,
                    topColor, bottomColor,
                    Shader.TileMode.CLAMP
                )

                val r = min(cornerRadiusPx, min((right - left) / 2f, barHeight / 2f))
                val radii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                barRect.set(left, top, right, bottom)
                barPath.reset()
                barPath.addRoundRect(barRect, radii, Path.Direction.CW)
                c.drawPath(barPath, mRenderPaint)

                // Viền sáng mảnh ở đỉnh cột (top specular highlight)
                rimPaint.color = ColorUtils.setAlphaComponent(
                    ColorUtils.blendARGB(baseColor, Color.WHITE, 0.55f),
                    0xB8
                )
                c.drawRoundRect(
                    left + 0.5f,
                    top + 0.5f,
                    right - 0.5f,
                    min(bottom, top + r * 1.6f),
                    r,
                    r,
                    rimPaint
                )
            }
            j += 4
        }
        mRenderPaint.shader = null
    }
}
