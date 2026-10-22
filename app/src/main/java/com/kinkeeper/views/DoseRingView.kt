package com.kinkeeper.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.kinkeeper.R

/**
 * The CustomView. A ring showing how many of today's doses are done, with the
 * count in the middle, drawn entirely by hand onto a Canvas.
 *
 * onMeasure forces the view square so the ring is a circle, not an ellipse.
 * onDraw draws a grey full circle, then an arc over it covering the fraction
 * taken. The arc starts at -90 degrees because 0 degrees in Android points at
 * three o'clock, and a progress ring should start at twelve.
 *
 * setProgress() animates with a ValueAnimator rather than jumping; each frame
 * it calls invalidate(), which is how you ask Android to run onDraw again.
 *
 * The Paint objects are properties, not created inside onDraw, because onDraw
 * runs up to 60 times a second and allocating there causes visible stutter.
 */
class DoseRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var taken = 0
    private var total = 0
    private var sweepFraction = 0f
    private var animator: ValueAnimator? = null

    private val density = resources.displayMetrics.density
    private val ringWidth = 14f * density
    private val bounds = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ringWidth
        color = ContextCompat.getColor(context, R.color.track)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ringWidth
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.primary)
    }

    private val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.ink)
        textSize = 34f * density
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.slate)
        textSize = 13f * density
    }

    /** Called by HomeActivity whenever the schedule changes. */
    fun setProgress(taken: Int, total: Int) {
        this.taken = taken
        this.total = total
        val target = if (total == 0) 0f else taken.toFloat() / total.toFloat()

        progressPaint.color = ContextCompat.getColor(
            context,
            if (total > 0 && taken == total) R.color.ok else R.color.primary
        )

        animator?.cancel()
        animator = ValueAnimator.ofFloat(sweepFraction, target).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                sweepFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    // Keep the view square, whatever the parent offers.
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val size = if (h == 0) w else minOf(w, h)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val pad = ringWidth / 2f + 2f * density
        bounds.set(pad, pad, width - pad, height - pad)

        // full circle behind
        canvas.drawArc(bounds, 0f, 360f, false, trackPaint)

        // the part that is done, starting at twelve o'clock
        if (sweepFraction > 0f) {
            canvas.drawArc(bounds, -90f, 360f * sweepFraction, false, progressPaint)
        }

        // numbers in the middle
        val centreX = width / 2f
        val centreY = height / 2f
        val countText = if (total == 0) "—" else "$taken/$total"
        canvas.drawText(countText, centreX, centreY + 6f * density, countPaint)
        canvas.drawText(
            context.getString(R.string.doses_today),
            centreX,
            centreY + 28f * density,
            labelPaint
        )
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
