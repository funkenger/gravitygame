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

    data class State(val up: Boolean = false, val down: Boolean = false, val left: Boolean = false, val right: Boolean = false)

    var visualize = true
    var onState: (State) -> Unit = {}

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 1f
    private var state = State()

    private val activePointers = linkedMapOf<Int, Pair<Float, Float>>()

    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = Color.argb(170, 40, 40, 40) }
    private val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.argb(120, 70, 70, 70) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.argb(80, 0, 150, 255) }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2f
        centerY = h / 2f
        radius = minOf(w, h) * 0.45f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    activePointers[event.getPointerId(i)] = event.getX(i) to event.getY(i)
                }
                recalc()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                activePointers.remove(event.getPointerId(event.actionIndex))
                recalc()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointers.clear()
                recalc()
            }
        }
        return true
    }

    private fun recalc() {
        var up = false
        var down = false
        var left = false
        var right = false
        activePointers.values.forEach { (x, y) ->
            val dx = x - centerX
            val dy = y - centerY
            val mag = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (mag < radius * 0.2f) return@forEach
            val ang = atan2(dy, dx)
            val sector = (((ang + Math.PI) / (Math.PI / 4.0)).toInt()) % 8
            when (sector) {
                0 -> left = true
                1 -> { left = true; up = true }
                2 -> up = true
                3 -> { right = true; up = true }
                4 -> right = true
                5 -> { right = true; down = true }
                6 -> down = true
                7 -> { left = true; down = true }
            }
        }
        val next = State(up, down, left, right)
        if (next != state) {
            state = next
            onState(state)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!visualize) return
        canvas.drawCircle(centerX, centerY, radius, ring)
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, guide)
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, guide)
        canvas.drawLine(centerX - radius * 0.7f, centerY - radius * 0.7f, centerX + radius * 0.7f, centerY + radius * 0.7f, guide)
        canvas.drawLine(centerX + radius * 0.7f, centerY - radius * 0.7f, centerX - radius * 0.7f, centerY + radius * 0.7f, guide)

        val knobX = when {
            state.left -> centerX - radius * 0.45f
            state.right -> centerX + radius * 0.45f
            else -> centerX
        }
        val knobY = when {
            state.up -> centerY - radius * 0.45f
            state.down -> centerY + radius * 0.45f
            else -> centerY
        }
        canvas.drawCircle(knobX, knobY, radius * 0.23f, fill)
    }
}
