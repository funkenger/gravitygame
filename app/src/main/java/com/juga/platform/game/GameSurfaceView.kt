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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
    private var crashDispatched = false
    private var finishDispatched = false

    var checkpointsEnabled = true
    var ghostVisible = true
    var dustEnabled = true

    private val ghostRun = mutableListOf<BikeSnapshot>()
    var loadedGhost: List<BikeSnapshot> = emptyList()

    private data class Dust(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float)
    private val dust = ArrayDeque<Dust>()

    var onHud: (time: Float, x: Float) -> Unit = { _, _ -> }
    var onFinished: (Result, List<BikeSnapshot>) -> Unit = { _, _ -> }
    var onCrashed: () -> Unit = {}

    private val paintSkyTop = Paint().apply { color = Color.rgb(112, 176, 255) }
    private val paintSkyBottom = Paint().apply { color = Color.rgb(196, 232, 255) }
    private val paintHillFar = Paint().apply { color = Color.rgb(137, 179, 160) }
    private val paintHillNear = Paint().apply { color = Color.rgb(95, 152, 127) }
    private val paintTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(68, 94, 68); strokeWidth = 12f; style = Paint.Style.STROKE }
    private val paintTrackEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(22, 46, 22); strokeWidth = 3f; style = Paint.Style.STROKE }
    private val paintFrame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(251, 205, 46); strokeWidth = 8f; style = Paint.Style.STROKE }
    private val paintWheel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 30, 30); strokeWidth = 6f; style = Paint.Style.STROKE }
    private val paintRim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(229, 229, 229); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val paintRider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(232, 80, 70); style = Paint.Style.FILL }
    private val paintGhost = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 130, 255, 240); strokeWidth = 6f; style = Paint.Style.STROKE }
    private val paintGhostWheel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 60, 200, 255); strokeWidth = 4f; style = Paint.Style.STROKE }
    private val paintDust = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 120, 108, 82); style = Paint.Style.FILL }
    private val paintFlagPole = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(220, 230, 235); strokeWidth = 6f }
    private val paintFlag = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(214, 44, 32); style = Paint.Style.FILL }

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    fun loadLevel(level: Level) {
        this.level = level
        this.bike = BikePhysics(level.start.x, level.start.y)
        timerStarted = false
        timeSec = 0f
        checkpointPenalty = 0f
        lastCheckpointX = level.start.x
        crashDispatched = false
        finishDispatched = false
        ghostRun.clear()
        dust.clear()
    }

    fun restartLevel() {
        level?.let(::loadLevel)
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

    fun setPaused(v: Boolean) {
        paused = v
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        loopThread = Thread(::loop, "game-loop").also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        loopThread?.join(800)
    }

    private fun loop() {
        val dt = 1f / 60f
        var prev = System.nanoTime()
        var acc = 0f

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

        val hasInput = inputState.throttle || inputState.brake || inputState.leanBack || inputState.leanForward
        if (hasInput) timerStarted = true

        if (timerStarted && !bike.finished && !bike.crashed) {
            timeSec += dt
        }

        bike.step(dt, level, inputState)

        if (timerStarted && (ghostRun.isEmpty() || timeSec - ghostRun.last().t >= 0.05f)) {
            ghostRun += bike.snapshot(timeSec)
        }

        if (dustEnabled && inputState.throttle && dust.size < 140) {
            val sx = bike.rear.pos.x - 0.2f
            val sy = bike.rear.pos.y + 0.34f
            repeat(2) {
                dust.add(
                    Dust(
                        x = sx,
                        y = sy,
                        vx = -2.2f - (it * 0.35f),
                        vy = -1.0f - (it * 0.2f),
                        life = 0.65f
                    )
                )
            }
        }
        val it = dust.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            p.vy += 5.5f * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vx *= 0.985f
            if (p.life <= 0f) it.remove()
        }

        if (checkpointsEnabled && level.checkpoints.isNotEmpty()) {
            level.checkpoints.forEach { cp ->
                if (bike.frame.pos.x > cp.x && cp.x > lastCheckpointX) lastCheckpointX = cp.x
            }
        }

        onHud(timeSec + checkpointPenalty, bike.frame.pos.x)

        if (bike.crashed && !crashDispatched) {
            if (checkpointsEnabled && lastCheckpointX > level.start.x + 0.5f) {
                checkpointPenalty += 1f
                bike.frame.pos.x = lastCheckpointX
                bike.frame.pos.y = level.start.y - 0.55f
                bike.rear.pos.x = lastCheckpointX - 0.78f
                bike.rear.pos.y = level.start.y
                bike.front.pos.x = lastCheckpointX + 0.78f
                bike.front.pos.y = level.start.y
                bike.frame.vel.x = 0f; bike.frame.vel.y = 0f
                bike.rear.vel.x = 0f; bike.rear.vel.y = 0f
                bike.front.vel.x = 0f; bike.front.vel.y = 0f
                bike.crashed = false
            } else {
                crashDispatched = true
                onCrashed()
            }
        }

        if (bike.finished && !finishDispatched) {
            finishDispatched = true
            val total = timeSec + checkpointPenalty
            val medal = when {
                total <= level.medals.gold -> MedalType.GOLD
                total <= level.medals.silver -> MedalType.SILVER
                total <= level.medals.bronze -> MedalType.BRONZE
                else -> MedalType.NONE
            }
            onFinished(Result(level.id, total, medal, finished = true, crashed = false, screenshot = captureBitmap()), ghostRun.toList())
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
            val worldToPx = h / 15.5f
            val cameraX = bike.frame.pos.x - 4f

            c.drawRect(0f, 0f, w, h * 0.5f, paintSkyTop)
            c.drawRect(0f, h * 0.5f, w, h, paintSkyBottom)

            val farShift = (cameraX * 20f) % w
            val nearShift = (cameraX * 38f) % w
            c.drawRect(-farShift, h * 0.40f, w, h * 0.62f, paintHillFar)
            c.drawRect(w - farShift, h * 0.40f, w * 2f, h * 0.62f, paintHillFar)
            c.drawRect(-nearShift, h * 0.50f, w, h * 0.74f, paintHillNear)
            c.drawRect(w - nearShift, h * 0.50f, w * 2f, h * 0.74f, paintHillNear)

            for (s in level.segments) {
                val x1 = (s.x1 - cameraX) * worldToPx
                val y1 = s.y1 * worldToPx
                val x2 = (s.x2 - cameraX) * worldToPx
                val y2 = s.y2 * worldToPx
                c.drawLine(x1, y1, x2, y2, paintTrack)
                c.drawLine(x1, y1, x2, y2, paintTrackEdge)
            }

            for (p in dust) {
                val px = (p.x - cameraX) * worldToPx
                val py = p.y * worldToPx
                val r = (0.1f + p.life * 0.18f) * worldToPx
                c.drawCircle(px, py, r, paintDust)
            }

            val finishX = (level.finishX - cameraX) * worldToPx
            c.drawLine(finishX, 2.4f * worldToPx, finishX, 9.2f * worldToPx, paintFlagPole)
            c.drawRect(finishX, 2.4f * worldToPx, finishX + 0.82f * worldToPx, 3.35f * worldToPx, paintFlag)

            if (ghostVisible && loadedGhost.isNotEmpty()) {
                val idx = (timeSec / 0.05f).roundToInt().coerceIn(0, loadedGhost.lastIndex)
                val g = loadedGhost[idx]
                drawBike(
                    c,
                    cameraX,
                    worldToPx,
                    g.frameX,
                    g.frameY,
                    g.backX,
                    g.backY,
                    g.frontX,
                    g.frontY,
                    framePaint = paintGhost,
                    wheelPaint = paintGhostWheel,
                    rimPaint = paintGhostWheel,
                    riderPaint = paintGhostWheel,
                    rearSpin = 0f,
                    frontSpin = 0f
                )
            }

            drawBike(
                c,
                cameraX,
                worldToPx,
                bike.frame.pos.x,
                bike.frame.pos.y,
                bike.rear.pos.x,
                bike.rear.pos.y,
                bike.front.pos.x,
                bike.front.pos.y,
                framePaint = paintFrame,
                wheelPaint = paintWheel,
                rimPaint = paintRim,
                riderPaint = paintRider,
                rearSpin = bike.rearSpin,
                frontSpin = bike.frontSpin
            )
        } finally {
            holder.unlockCanvasAndPost(c)
        }
    }

    private fun drawBike(
        c: Canvas,
        cameraX: Float,
        worldToPx: Float,
        fx: Float,
        fy: Float,
        rx: Float,
        ry: Float,
        nx: Float,
        ny: Float,
        framePaint: Paint,
        wheelPaint: Paint,
        rimPaint: Paint,
        riderPaint: Paint,
        rearSpin: Float,
        frontSpin: Float
    ) {
        val fpx = (fx - cameraX) * worldToPx
        val fpy = fy * worldToPx
        val rpx = (rx - cameraX) * worldToPx
        val rpy = ry * worldToPx
        val npx = (nx - cameraX) * worldToPx
        val npy = ny * worldToPx
        val wr = 0.46f * worldToPx

        c.drawLine(rpx, rpy, fpx, fpy, framePaint)
        c.drawLine(fpx, fpy, npx, npy, framePaint)
        c.drawLine(rpx, rpy, npx, npy, framePaint)

        c.drawCircle(rpx, rpy, wr, wheelPaint)
        c.drawCircle(npx, npy, wr, wheelPaint)

        drawSpokes(c, rpx, rpy, wr * 0.8f, rearSpin, rimPaint)
        drawSpokes(c, npx, npy, wr * 0.8f, frontSpin, rimPaint)

        val riderX = fpx
        val riderY = fpy - 0.32f * worldToPx
        c.drawCircle(riderX, riderY - 0.20f * worldToPx, 0.10f * worldToPx, riderPaint)
        c.drawLine(riderX, riderY - 0.09f * worldToPx, riderX, riderY + 0.24f * worldToPx, riderPaint)
    }

    private fun drawSpokes(c: Canvas, x: Float, y: Float, r: Float, spin: Float, p: Paint) {
        val a1 = spin
        val a2 = spin + (Math.PI.toFloat() / 2f)
        c.drawLine(x, y, x + cos(a1) * r, y + sin(a1) * r, p)
        c.drawLine(x, y, x + cos(a2) * r, y + sin(a2) * r, p)
    }

    fun captureBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        draw(canvas)
        return bmp
    }
}
