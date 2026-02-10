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
    private val pDebugBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(160, 255, 255, 255); style = Paint.Style.FILL }
    private val pDebug = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 24f }

    var camX = 0f
    var camY = 6f

    fun draw(
        canvas: Canvas,
        state: GameState,
        showGhost: Boolean,
        debugOverlay: Boolean,
        verticalCentering: Boolean,
        hudPx: Float,
        dpadPx: Float
    ) {
        val w = canvas.width.toFloat().coerceAtLeast(1f)
        val h = canvas.height.toFloat().coerceAtLeast(1f)
        val ppm = 30f

        val viewportTop = hudPx
        val viewportBottom = h - dpadPx
        val viewportCenterY = (viewportTop + viewportBottom) * 0.5f
        val viewportCenterX = w * 0.5f

        canvas.drawColor(Color.WHITE)

        val tx = state.world.chassis.position.x
        val targetY = if (verticalCentering) state.world.chassis.position.y - 0.3f else state.world.chassis.position.y - 3.8f
        camX += (tx - camX) * 0.14f
        camY += (targetY - camY) * 0.10f
        camY = camY.coerceIn(4f, 10f)

        fun sx(x: Float) = (x - camX) * ppm + viewportCenterX
        fun sy(y: Float) = (y - camY) * ppm + viewportCenterY

        state.level.segments.forEach {
            canvas.drawLine(sx(it.x1), sy(it.y1), sx(it.x2), sy(it.y2), pTrack)
        }

        val fx = sx(state.level.finishX)
        canvas.drawLine(fx, sy(2f), fx, sy(10f), pFlag)
        canvas.drawLine(fx, sy(2.2f), fx + 10f, sy(2.6f), pFlag)

        if (showGhost && state.loadedGhost.isNotEmpty()) {
            val i = (state.timer / 0.05f).roundToInt().coerceIn(0, state.loadedGhost.lastIndex)
            drawBike(canvas, state.loadedGhost[i], ::sx, ::sy, pGhost, pGhost, pGhost)
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
        drawBike(canvas, player, ::sx, ::sy, pBike, pWheel, pAccent)

        if (debugOverlay) {
            canvas.drawRect(8f, viewportTop + 8f, w - 8f, viewportTop + 250f, pDebugBg)
            val lines = listOf(
                "dir up=${state.input.up} dn=${state.input.down} l=${state.input.left} r=${state.input.right}",
                "ground rear=${state.world.rearGrounded} front=${state.world.frontGrounded}",
                "impulse N=${"%.2f".format(state.world.lastRearNormalImpulse)} T=${"%.2f".format(state.world.lastRearFrictionImpulse)}",
                "bike x=${"%.2f".format(state.world.chassis.position.x)} y=${"%.2f".format(state.world.chassis.position.y)} vx=${"%.2f".format(state.world.chassis.velocity.x)}",
                "camY=${"%.2f".format(camY)} centerY=${"%.1f".format(viewportCenterY)} scale=$ppm",
                "self-check: ${state.selfCheckWarning}"
            )
            lines.forEachIndexed { i, s -> canvas.drawText(s, 18f, viewportTop + 38f + i * 34f, pDebug) }
        }
    }

    private fun drawBike(
        canvas: Canvas,
        s: GhostSample,
        sx: (Float) -> Float,
        sy: (Float) -> Float,
        frame: Paint,
        wheel: Paint,
        accent: Paint
    ) {
        val cx = sx(s.cx)
        val cy = sy(s.cy)
        val rx = sx(s.rx)
        val ry = sy(s.ry)
        val fx = sx(s.fx)
        val fy = sy(s.fy)
        val r = 0.35f * 30f

        canvas.drawLine(rx, ry, cx, cy, frame)
        canvas.drawLine(cx, cy, fx, fy, frame)
        canvas.drawCircle(rx, ry, r, wheel)
        canvas.drawCircle(fx, fy, r, wheel)
        canvas.drawCircle(rx + cos(s.ca) * 3f, ry + sin(s.ca) * 3f, 2.4f, accent)
        canvas.drawCircle(fx + cos(-s.ca) * 3f, fy + sin(-s.ca) * 3f, 2.4f, accent)
    }
}
