package com.juga.platform.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

class DPadInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class State(
        val up: Boolean = false,
        val down: Boolean = false,
        val left: Boolean = false,
        val right: Boolean = false
    )

    var visualEnabled = true
    var onStateChanged: (State) -> Unit = {}

    private var cx = 0f
    private var cy = 0f
    private var radius = 1f
    private var state = State()

    private val pRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(180, 255, 255, 255)
        strokeWidth = 6f
    }
    private val pGuide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(120, 220, 235, 255)
        strokeWidth = 3f
    }
    private val pKnob = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(130, 70, 140, 255)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cx = w / 2f
        cy = h / 2f
        radius = minOf(w, h) * 0.43f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - cx
                val dy = event.y - cy
                val mag = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val dead = radius * 0.18f
                val nx = (dx / radius).coerceIn(-1f, 1f)
                val ny = (dy / radius).coerceIn(-1f, 1f)

                val next = if (mag < dead) {
                    State()
                } else {
                    val h = 0.35f
                    val left = nx < -h
                    val right = nx > h
                    val up = ny < -h
                    val down = ny > h
                    State(up = up, down = down, left = left, right = right)
                }
                updateState(next)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> updateState(State())
        }
        return true
    }

    private fun updateState(next: State) {
        if (next == state) return
        state = next
        onStateChanged(state)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (!visualEnabled) return

        canvas.drawCircle(cx, cy, radius, pRing)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, pGuide)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, pGuide)
        canvas.drawLine(cx - radius * 0.7f, cy - radius * 0.7f, cx + radius * 0.7f, cy + radius * 0.7f, pGuide)
        canvas.drawLine(cx + radius * 0.7f, cy - radius * 0.7f, cx - radius * 0.7f, cy + radius * 0.7f, pGuide)

        val knobX = when {
            state.left -> cx - radius * 0.48f
            state.right -> cx + radius * 0.48f
            else -> cx
        }
        val knobY = when {
            state.up -> cy - radius * 0.48f
            state.down -> cy + radius * 0.48f
            else -> cy
        }
        canvas.drawCircle(knobX, knobY, radius * 0.26f, pKnob)
    }
}
