package com.juga.platform.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.juga.platform.R
import com.juga.platform.data.GhostStore
import com.juga.platform.data.LevelLoader
import com.juga.platform.data.RecordsStore
import com.juga.platform.game.DPadInputView
import com.juga.platform.game.GameSurfaceView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameActivity : AppCompatActivity() {
    private lateinit var view: GameSurfaceView
    private var up = false
    private var down = false
    private var left = false
    private var right = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPortrait()
        setContentView(R.layout.activity_game)

        val levelId = intent.getIntExtra("level", 1)
        val loader = LevelLoader(this)
        val level = loader.level(levelId)
        val records = RecordsStore(this)
        val ghostStore = GhostStore(this)

        val levelText = findViewById<TextView>(R.id.txtLevel)
        val timer = findViewById<TextView>(R.id.txtTimer)
        val best = findViewById<TextView>(R.id.txtBest)
        levelText.text = "Level $levelId"
        view = findViewById(R.id.gameView)
        val dPad = findViewById<DPadInputView>(R.id.dpad)

        lifecycleScope.launch {
            best.text = "Best: ${formatTime(records.bestTime(levelId).first())}"
            view.showGhost = records.ghostEnabled.first()
            view.checkpointsEnabled = records.checkpointsEnabled.first()
            dPad.visualize = records.dPadVisible.first()
            view.debugOverlay = records.debugOverlayEnabled.first()
            view.verticalCentering = records.verticalCenteringEnabled.first()
            view.setGhostData(ghostStore.load(levelId))
        }

        view.loadLevel(level)
        dPad.onState = { view.input(it) }

        dPad.post {
            view.hudHeightPx = 56f * resources.displayMetrics.density
            view.dpadHeightPx = dPad.height.toFloat() + 20f * resources.displayMetrics.density
        }

        view.onHud = { t -> runOnUiThread { timer.text = formatTime(t) } }
        view.onCrash = {
            vibrate(90)
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle("Crash")
                    .setItems(arrayOf("Restart", "Exit")) { _, i -> if (i == 0) view.restart() else finish() }
                    .show()
            }
        }
        view.onFinish = { res, ghost ->
            lifecycleScope.launch {
                val improved = records.saveTime(levelId, res.time)
                if (improved) ghostStore.save(levelId, ghost)
                records.unlockNext(levelId, 20)
                vibrate(160)
                startActivity(
                    Intent(this@GameActivity, ResultsActivity::class.java)
                        .putExtra("level", levelId)
                        .putExtra("time", res.time)
                        .putExtra("medal", res.medal.name)
                )
                finish()
            }
        }

        findViewById<ImageButton>(R.id.btnRestart).setOnClickListener { view.restart() }
        findViewById<ImageButton>(R.id.btnPause).setOnClickListener { pauseDialog() }
    }

    private fun pauseDialog() {
        view.pauseGame(true)
        AlertDialog.Builder(this)
            .setTitle("Pause")
            .setItems(arrayOf("Resume", "Restart", "Exit")) { d, i ->
                when (i) {
                    0 -> view.pauseGame(false)
                    1 -> { view.restart(); view.pauseGame(false) }
                    2 -> finish()
                }
                d.dismiss()
            }
            .setOnDismissListener { view.pauseGame(false) }
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> up = true
            KeyEvent.KEYCODE_DPAD_DOWN -> down = true
            KeyEvent.KEYCODE_DPAD_LEFT -> left = true
            KeyEvent.KEYCODE_DPAD_RIGHT -> right = true
            else -> return super.onKeyDown(keyCode, event)
        }
        view.hardware(up, down, left, right)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> up = false
            KeyEvent.KEYCODE_DPAD_DOWN -> down = false
            KeyEvent.KEYCODE_DPAD_LEFT -> left = false
            KeyEvent.KEYCODE_DPAD_RIGHT -> right = false
            else -> return super.onKeyUp(keyCode, event)
        }
        view.hardware(up, down, left, right)
        return true
    }

    private fun vibrate(ms: Long) {
        val v = getSystemService<Vibrator>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") v.vibrate(ms)
    }
}
