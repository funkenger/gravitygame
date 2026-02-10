package com.juga.platform.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.juga.platform.data.LevelData
import com.juga.platform.data.Medal

class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    data class FinishData(val levelId: Int, val time: Float, val medal: Medal, val screenshot: Bitmap)

    private var state: GameState? = null
    private val renderer = GameRenderer()
    private lateinit var loop: GameLoop

    var showGhost = true
    var checkpointsEnabled = true
    var debugOverlay = true
    var verticalCentering = true
    var hudHeightPx = 76f
    var dpadHeightPx = 270f

    var onHud: (Float) -> Unit = {}
    var onCrash: () -> Unit = {}
    var onFinish: (FinishData, List<com.juga.platform.data.GhostSample>) -> Unit = { _, _ -> }

    private var crashSent = false
    private var finishSent = false

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun loadLevel(level: LevelData) {
        state = GameState(level)
        crashSent = false
        finishSent = false
    }

    fun setGhostData(samples: List<com.juga.platform.data.GhostSample>) {
        state?.loadedGhost = samples
    }

    fun input(input: DPadInputView.State) {
        state?.touchEvents = (state?.touchEvents ?: 0) + 1
        state?.input?.apply {
            up = input.up
            down = input.down
            left = input.left
            right = input.right
        }
    }

    fun hardware(up: Boolean, down: Boolean, left: Boolean, right: Boolean) {
        state?.input?.apply {
            this.up = up
            this.down = down
            this.left = left
            this.right = right
        }
    }

    fun pauseGame(pause: Boolean) {
        if (::loop.isInitialized) loop.paused = pause
    }

    fun restart() {
        val oldGhost = state?.loadedGhost.orEmpty()
        state?.let { loadLevel(it.level) }
        state?.loadedGhost = oldGhost
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        loop = GameLoop(
            tick = { dt ->
                val s = state ?: return@GameLoop
                s.update(dt, checkpointsEnabled)
                onHud(s.timer + s.checkpointPenalty)

                if (s.crashed && !crashSent) {
                    crashSent = true
                    onCrash()
                }
                if (s.finished && !finishSent) {
                    finishSent = true
                    onFinish(
                        FinishData(s.level.id, s.timer + s.checkpointPenalty, s.medal, captureBitmap()),
                        s.ghostRecording.toList()
                    )
                }
            },
            render = { drawFrame() }
        )
        loop.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (::loop.isInitialized) loop.stop()
    }

    private fun drawFrame() {
        val c = holder.lockCanvas() ?: return
        try {
            val s = state
            if (s != null) {
                renderer.draw(
                    canvas = c,
                    state = s,
                    showGhost = showGhost,
                    debugOverlay = debugOverlay,
                    verticalCentering = verticalCentering,
                    hudPx = hudHeightPx,
                    dpadPx = dpadHeightPx
                )
            }
        } finally {
            holder.unlockCanvasAndPost(c)
        }
    }

    fun captureBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        state?.let {
            renderer.draw(
                canvas = canvas,
                state = it,
                showGhost = showGhost,
                debugOverlay = debugOverlay,
                verticalCentering = verticalCentering,
                hudPx = hudHeightPx,
                dpadPx = dpadHeightPx
            )
        }
        return bmp
    }
}
