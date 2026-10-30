package com.example.studentwallet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5E677D")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1F36")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val rectF = RectF()
    private var slices: List<Float> = listOf(40f, 25f, 20f, 10f, 5f)
    private var totalText: String = "$364.50"

    private val palette = intArrayOf(
        Color.parseColor("#0B63F6"),
        Color.parseColor("#B3CCFF"),
        Color.parseColor("#08207A"),
        Color.parseColor("#B71C1C"),
        Color.parseColor("#E0E3EB"),
        Color.parseColor("#4F83CC")
    )

    fun setData(categoryValues: List<Double>, formattedTotal: String) {
        totalText = formattedTotal
        val sum = categoryValues.sum()
        slices = if (sum > 0) {
            categoryValues.map { it.toFloat() }
        } else {
            listOf(1f)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val stroke = size * 0.17f
        arcPaint.strokeWidth = stroke

        val padding = stroke / 2f + 12f
        val left = (width - size) / 2f + padding
        val top = (height - size) / 2f + padding
        val right = (width + size) / 2f - padding
        val bottom = (height + size) / 2f - padding
        rectF.set(left, top, right, bottom)

        val total = slices.sum()
        var startAngle = -90f

        if (total <= 0f || (slices.size == 1 && totalText.endsWith("0.00"))) {
            arcPaint.color = Color.parseColor("#E0E3EB")
            canvas.drawArc(rectF, 0f, 360f, false, arcPaint)
        } else {
            for (i in slices.indices) {
                val sweep = (slices[i] / total) * 360f
                arcPaint.color = palette[i % palette.size]
                val gap = if (slices.size > 1) 1.5f else 0f
                canvas.drawArc(rectF, startAngle, (sweep - gap).coerceAtLeast(1f), false, arcPaint)
                startAngle += sweep
            }
        }

        val centerX = width / 2f
        val centerY = height / 2f

        labelPaint.textSize = size * 0.068f
        valuePaint.textSize = size * 0.105f

        canvas.drawText("Total", centerX, centerY - (size * 0.035f), labelPaint)
        canvas.drawText(totalText, centerX, centerY + (size * 0.075f), valuePaint)
    }
}
