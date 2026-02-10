package com.juga.platform.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.juga.platform.data.GhostSample
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class GameRenderer {
    private val pTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0, 180, 0); strokeWidth = 2f }
    private val pBike = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 3f; style = Paint.Style.STROKE }
    private val pWheel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 2f; style = Paint.Style.STROKE }
    private val pAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED; style = Paint.Style.FILL }
    private val pGhost = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 0, 0, 255); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val pFlag = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 2f }

    var camX = 0f
    var camY = 6f

    fun draw(canvas: Canvas, state: GameState, showGhost: Boolean) {
        val w = canvas.width.toFloat().coerceAtLeast(1f)
        val h = canvas.height.toFloat().coerceAtLeast(1f)
        val ppm = 30f

        canvas.drawColor(Color.WHITE)

        val tx = state.world.chassis.position.x - 3.8f
        val ty = state.world.chassis.position.y - 6.5f
        camX += (tx - camX) * 0.12f
        camY += (ty - camY) * 0.08f
        camY = camY.coerceIn(2f, 12f)

        state.level.segments.forEach {
            canvas.drawLine(
                (it.x1 - camX) * ppm,
                (it.y1 - camY) * ppm,
                (it.x2 - camX) * ppm,
                (it.y2 - camY) * ppm,
                pTrack
            )
        }

        val fx = (state.level.finishX - camX) * ppm
        canvas.drawLine(fx, (2f - camY) * ppm, fx, (10f - camY) * ppm, pFlag)
        canvas.drawLine(fx, (2.2f - camY) * ppm, fx + 10f, (2.6f - camY) * ppm, pFlag)

        if (showGhost && state.loadedGhost.isNotEmpty()) {
            val i = (state.timer / 0.05f).roundToInt().coerceIn(0, state.loadedGhost.lastIndex)
            drawBike(canvas, state.loadedGhost[i], camX, camY, ppm, pGhost, pGhost, pGhost)
        }

        val player = GhostSample(
            t = state.timer,
            cx = state.world.chassis.position.x,
            cy = state.world.chassis.position.y,
            ca = state.world.chassis.angle,
            rx = state.world.rearWheel.position.x,
            ry = state.world.rearWheel.position.y,
            fx = state.world.frontWheel.position.x,
            fy = state.world.frontWheel.position.y
        )
        drawBike(canvas, player, camX, camY, ppm, pBike, pWheel, pAccent)
    }

    private fun drawBike(canvas: Canvas, s: GhostSample, camX: Float, camY: Float, ppm: Float, frame: Paint, wheel: Paint, accent: Paint) {
        val cx = (s.cx - camX) * ppm
        val cy = (s.cy - camY) * ppm
        val rx = (s.rx - camX) * ppm
        val ry = (s.ry - camY) * ppm
        val fx = (s.fx - camX) * ppm
        val fy = (s.fy - camY) * ppm
        val r = 0.35f * ppm

        canvas.drawLine(rx, ry, cx, cy, frame)
        canvas.drawLine(cx, cy, fx, fy, frame)
        canvas.drawCircle(rx, ry, r, wheel)
        canvas.drawCircle(fx, fy, r, wheel)
        canvas.drawCircle(rx + cos(s.ca) * 3f, ry + sin(s.ca) * 3f, 2.4f, accent)
        canvas.drawCircle(fx + cos(-s.ca) * 3f, fy + sin(-s.ca) * 3f, 2.4f, accent)
    }
}
