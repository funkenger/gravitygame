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
import com.juga.platform.data.GhostRepository
import com.juga.platform.data.LevelRepository
import com.juga.platform.data.PreferencesRepository
import com.juga.platform.game.DPadInputView
import com.juga.platform.game.GameSurfaceView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameActivity : AppCompatActivity() {
    private lateinit var gameView: GameSurfaceView
    private lateinit var dpad: DPadInputView
    private var up = false
    private var down = false
    private var left = false
    private var right = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPortrait()
        setContentView(R.layout.activity_game)

        val timer = findViewById<TextView>(R.id.txtTimer)
        val best = findViewById<TextView>(R.id.txtBest)
        gameView = findViewById(R.id.gameView)
        dpad = findViewById(R.id.dpad)

        val levelId = intent.getIntExtra("level", 1)
        val levels = LevelRepository(this).loadAllLevels()
        val level = levels.first { it.id == levelId }
        val prefs = PreferencesRepository(this)
        val ghostRepo = GhostRepository(this)

        lifecycleScope.launch {
            best.text = "Best: ${formatTime(prefs.bestTime(levelId).first())}"
            dpad.visualEnabled = prefs.showPad.first()
            gameView.ghostVisible = prefs.showGhost.first()
            gameView.loadedGhost = ghostRepo.load(levelId)
            gameView.checkpointsEnabled = prefs.checkpointsEnabled.first()
            gameView.dustEnabled = prefs.dustEnabled.first()
        }

        gameView.loadLevel(level)

        dpad.onStateChanged = {
            gameView.setInput(it)
        }

        gameView.onHud = { t, _ -> runOnUiThread { timer.text = formatTime(t) } }
        gameView.onCrashed = {
            buzz(100)
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle("Crash")
                    .setMessage("Try again?")
                    .setPositiveButton("Restart") { _, _ -> gameView.restartLevel() }
                    .setNegativeButton("Quit") { _, _ -> finish() }
                    .show()
            }
        }
        gameView.onFinished = { result, ghost ->
            buzz(180)
            lifecycleScope.launch {
                val improved = prefs.saveBestTime(levelId, result.timeSec)
                if (improved) ghostRepo.save(levelId, ghost)
                prefs.setHighestUnlocked((levelId + 1).coerceAtMost(20))
                val out = Intent(this@GameActivity, ResultsActivity::class.java)
                    .putExtra("level", levelId)
                    .putExtra("time", result.timeSec)
                    .putExtra("medal", result.medal.name)
                startActivity(out)
                finish()
            }
        }

        findViewById<ImageButton>(R.id.btnRestart).setOnClickListener { gameView.restartLevel() }
        findViewById<ImageButton>(R.id.btnPause).setOnClickListener { showPauseDialog() }
    }

    private fun showPauseDialog() {
        gameView.setPaused(true)
        AlertDialog.Builder(this)
            .setTitle("Paused")
            .setItems(arrayOf("Resume", "Restart", "Quit")) { d, which ->
                when (which) {
                    0 -> gameView.setPaused(false)
                    1 -> {
                        gameView.restartLevel(); gameView.setPaused(false)
                    }
                    2 -> finish()
                }
                d.dismiss()
            }
            .setOnDismissListener { gameView.setPaused(false) }
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
        gameView.setHardwareInput(up, down, left, right)
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
        gameView.setHardwareInput(up, down, left, right)
        return true
    }

    private fun buzz(ms: Long) {
        val v = getSystemService<Vibrator>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") v.vibrate(ms)
        }
    }
}
