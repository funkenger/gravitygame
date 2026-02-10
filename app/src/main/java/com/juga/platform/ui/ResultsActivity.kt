package com.juga.platform.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.juga.platform.R
import com.juga.platform.data.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ResultsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPortrait()
        setContentView(R.layout.activity_results)

        val level = intent.getIntExtra("level", 1)
        val time = intent.getFloatExtra("time", 0f)
        val medal = intent.getStringExtra("medal") ?: "NONE"

        findViewById<TextView>(R.id.txtTime).text = "Time: ${formatTime(time)}"
        findViewById<TextView>(R.id.txtMedal).text = "Medal: $medal"

        lifecycleScope.launch {
            val best = PreferencesRepository(this@ResultsActivity).bestTime(level).first()
            findViewById<TextView>(R.id.txtBest).text = "Best: ${formatTime(best)}"
        }

        findViewById<Button>(R.id.btnShare).setOnClickListener { share(level, time) }
        findViewById<Button>(R.id.btnReplay).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java).putExtra("level", level))
            finish()
        }
        findViewById<Button>(R.id.btnNext).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java).putExtra("level", (level + 1).coerceAtMost(20)))
            finish()
        }
        findViewById<Button>(R.id.btnQuit).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }
    }

    private fun share(level: Int, time: Float) {
        val text = "JugaPlatform — I beat Level $level in ${formatTime(time)}!"
        val uri = createShareImage(level, time)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(i, "Share result"))
    }

    private fun createShareImage(level: Int, time: Float): Uri {
        val bmp = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(24, 34, 64))
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 64f }
        c.drawText("JugaPlatform", 80f, 180f, p)
        p.textSize = 54f
        c.drawText("Level $level", 80f, 320f, p)
        c.drawText("Time ${formatTime(time)}", 80f, 420f, p)

        val dir = File(cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "result_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(this, "com.juga.platform.fileprovider", file)
    }
}
