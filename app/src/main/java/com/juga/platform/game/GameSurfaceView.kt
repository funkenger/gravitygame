package com.juga.platform.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.juga.platform.model.Level
import com.juga.platform.model.MedalType
import com.juga.platform.physics.BikeInput
import com.juga.platform.physics.BikePhysics
import com.juga.platform.physics.BikeSnapshot
import kotlin.math.roundToInt

class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    data class Result(
        val levelId: Int,
        val timeSec: Float,
        val medal: MedalType,
        val finished: Boolean,
        val crashed: Boolean,
        val screenshot: Bitmap
    )

    private var level: Level? = null
    private var bike: BikePhysics? = null
    private var loopThread: Thread? = null
    @Volatile private var running = false
    @Volatile private var paused = false

    private var inputState = BikeInput()
    private var timerStarted = false
    private var timeSec = 0f
    private var checkpointPenalty = 0f
    private var lastCheckpointX = 0f
    var checkpointsEnabled = true
    var ghostVisible = true
    var dustEnabled = true

    private val ghostRun = mutableListOf<BikeSnapshot>()
    var loadedGhost: List<BikeSnapshot> = emptyList()

    var onHud: (time: Float, x: Float) -> Unit = { _, _ -> }
    var onFinished: (Result, List<BikeSnapshot>) -> Unit = { _, _ -> }
    var onCrashed: () -> Unit = {}

    private val paintTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; strokeWidth = 8f }
    private val paintBike = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.YELLOW; strokeWidth = 6f }
    private val paintWheel = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.DKGRAY; strokeWidth = 5f }
    private val paintGhost = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 100, 220, 255); strokeWidth = 5f }
    private val paintBg1 = Paint().apply { color = Color.rgb(25, 35, 80) }
    private val paintBg2 = Paint().apply { color = Color.rgb(35, 90, 130) }
    private val paintFlag = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED; strokeWidth = 5f }

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    fun loadLevel(level: Level) {
        this.level = level
        this.bike = BikePhysics(level.start.x, level.start.y)
        timeSec = 0f
        checkpointPenalty = 0f
        timerStarted = false
        lastCheckpointX = level.start.x
        ghostRun.clear()
    }

    fun restartLevel() {
        level?.let { loadLevel(it) }
    }

    fun setInput(state: DPadInputView.State) {
        inputState = BikeInput(
            throttle = state.up,
            brake = state.down,
            leanBack = state.left,
            leanForward = state.right
        )
    }

    fun setHardwareInput(up: Boolean, down: Boolean, left: Boolean, right: Boolean) {
        inputState = BikeInput(up, down, left, right)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        loopThread = Thread(::loop, "game-loop").also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        loopThread?.join(700)
    }

    fun setPaused(v: Boolean) {
        paused = v
    }

    private fun loop() {
        val dt = 1f / 60f
        var acc = 0f
        var prev = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            acc += (now - prev) / 1_000_000_000f
            prev = now

            while (acc >= dt) {
                update(dt)
                acc -= dt
            }
            drawFrame()
        }
    }

    private fun update(dt: Float) {
        if (paused) return
        val level = level ?: return
        val bike = bike ?: return

        val moved = inputState.throttle || inputState.brake || inputState.leanBack || inputState.leanForward
        if (moved) timerStarted = true
        if (timerStarted && !bike.crashed && !bike.finished) timeSec += dt

        bike.step(dt, level, inputState)
        if (timerStarted && (ghostRun.isEmpty() || timeSec - ghostRun.last().t >= 0.05f)) {
            ghostRun += bike.snapshot(timeSec)
        }

        if (checkpointsEnabled && level.checkpoints.isNotEmpty()) {
            for (cp in level.checkpoints) {
                if (bike.frame.pos.x > cp.x && cp.x > lastCheckpointX) lastCheckpointX = cp.x
            }
        }

        onHud(timeSec + checkpointPenalty, bike.frame.pos.x)

        if (bike.crashed) {
            if (checkpointsEnabled && lastCheckpointX > level.start.x + 0.5f) {
                checkpointPenalty += 1f
                bike.frame.pos.x = lastCheckpointX
                bike.frame.pos.y = level.start.y
                bike.front.pos.x = lastCheckpointX + 0.8f
                bike.front.pos.y = level.start.y + 0.2f
                bike.rear.pos.x = lastCheckpointX - 0.8f
                bike.rear.pos.y = level.start.y + 0.2f
                bike.frame.vel.x = 0f; bike.frame.vel.y = 0f
                bike.front.vel.x = 0f; bike.front.vel.y = 0f
                bike.rear.vel.x = 0f; bike.rear.vel.y = 0f
                bike.crashed = false
            } else {
                onCrashed()
            }
        }
        if (bike.finished) {
            val total = timeSec + checkpointPenalty
            val medal = when {
                total <= level.medals.gold -> MedalType.GOLD
                total <= level.medals.silver -> MedalType.SILVER
                total <= level.medals.bronze -> MedalType.BRONZE
                else -> MedalType.NONE
            }
            onFinished(Result(level.id, total, medal, true, false, captureBitmap()), ghostRun.toList())
        }
    }

    private fun drawFrame() {
        val c = holder.lockCanvas() ?: return
        try {
            val level = level
            val bike = bike
            if (level == null || bike == null) {
                c.drawColor(Color.BLACK)
                return
            }
            val w = width.toFloat().coerceAtLeast(1f)
            val h = height.toFloat().coerceAtLeast(1f)
            val scale = h / 14f
            val cameraX = bike.frame.pos.x - 4f

            c.drawRect(0f, 0f, w, h, paintBg1)
            c.drawRect(-((cameraX * 25) % w), h * 0.2f, w, h * 0.35f, paintBg2)
            c.drawRect(-((cameraX * 35) % w), h * 0.38f, w, h * 0.48f, paintBg2)

            level.segments.forEach {
                c.drawLine((it.x1 - cameraX) * scale, it.y1 * scale, (it.x2 - cameraX) * scale, it.y2 * scale, paintTrack)
            }

            val fx = (level.finishX - cameraX) * scale
            c.drawLine(fx, 3f * scale, fx, 9f * scale, paintFlag)
            c.drawRect(fx, 3f * scale, fx + 0.6f * scale, 3.8f * scale, paintFlag)

            if (ghostVisible && loadedGhost.isNotEmpty()) {
                val idx = ((timeSec / 0.05f).roundToInt()).coerceIn(0, loadedGhost.lastIndex)
                val g = loadedGhost[idx]
                drawBike(c, g.frameX, g.frameY, g.backX, g.backY, g.frontX, g.frontY, cameraX, scale, paintGhost, paintGhost)
            }
            drawBike(c, bike.frame.pos.x, bike.frame.pos.y, bike.rear.pos.x, bike.rear.pos.y, bike.front.pos.x, bike.front.pos.y, cameraX, scale, paintBike, paintWheel)
        } finally {
            holder.unlockCanvasAndPost(c)
        }
    }

    private fun drawBike(
        c: Canvas,
        fx: Float,
        fy: Float,
        rx: Float,
        ry: Float,
        nx: Float,
        ny: Float,
        cameraX: Float,
        scale: Float,
        framePaint: Paint,
        wheelPaint: Paint
    ) {
        val fxs = (fx - cameraX) * scale
        val fys = fy * scale
        val rxs = (rx - cameraX) * scale
        val rys = ry * scale
        val nxs = (nx - cameraX) * scale
        val nys = ny * scale

        c.drawLine(rxs, rys, fxs, fys, framePaint)
        c.drawLine(fxs, fys, nxs, nys, framePaint)
        c.drawCircle(rxs, rys, 0.45f * scale, wheelPaint)
        c.drawCircle(nxs, nys, 0.45f * scale, wheelPaint)
        c.drawCircle(fxs, fys, 0.18f * scale, framePaint)
    }

    fun captureBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        draw(c)
        return bmp
    }
}
