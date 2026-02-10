package com.juga.platform.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
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

    private val pStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(180, 255, 255, 255)
        strokeWidth = 6f
    }
    private val pFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(90, 80, 160, 255)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cx = w / 2f
        cy = h / 2f
        radius = minOf(w, h) * 0.42f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - cx
                val dy = event.y - cy
                val mag = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val dead = radius * 0.2f
                val s = if (mag < dead) {
                    State()
                } else {
                    val angle = atan2(dy, dx)
                    val sector = (((angle + Math.PI) / (Math.PI / 4.0)).toInt()) % 8
                    when (sector) {
                        0 -> State(left = true)
                        1 -> State(left = true, up = true)
                        2 -> State(up = true)
                        3 -> State(right = true, up = true)
                        4 -> State(right = true)
                        5 -> State(right = true, down = true)
                        6 -> State(down = true)
                        else -> State(left = true, down = true)
                    }
                }
                updateState(s)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> updateState(State())
        }
        return true
    }

    private fun updateState(next: State) {
        if (next != state) {
            state = next
            onStateChanged(state)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!visualEnabled) return
        canvas.drawCircle(cx, cy, radius, pStroke)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, pStroke)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, pStroke)

        val knobX = when {
            state.left -> cx - radius * 0.5f
            state.right -> cx + radius * 0.5f
            else -> cx
        }
        val knobY = when {
            state.up -> cy - radius * 0.5f
            state.down -> cy + radius * 0.5f
            else -> cy
        }
        canvas.drawCircle(knobX, knobY, radius * 0.25f, pFill)
    }
}
