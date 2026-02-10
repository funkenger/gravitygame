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
import com.juga.platform.data.RecordsStore
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
            val best = RecordsStore(this@ResultsActivity).bestTime(level).first()
            findViewById<TextView>(R.id.txtBest).text = "Best: ${formatTime(best)}"
        }

        findViewById<Button>(R.id.btnShare).setOnClickListener { share(level, time) }
        findViewById<Button>(R.id.btnReplay).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java).putExtra("level", level)); finish()
        }
        findViewById<Button>(R.id.btnNext).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java).putExtra("level", (level + 1).coerceAtMost(20))); finish()
        }
        findViewById<Button>(R.id.btnQuit).setOnClickListener {
            startActivity(Intent(this, TitleActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); finish()
        }
    }

    private fun share(level: Int, time: Float) {
        val text = "JugaPlatform: Level $level — ${formatTime(time)}"
        val uri = image(level, time)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(i, "Share"))
    }

    private fun image(level: Int, time: Float): Uri {
        val bmp = Bitmap.createBitmap(720, 720, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 52f }
        c.drawText("JugaPlatform", 42f, 120f, p)
        c.drawText("Level $level", 42f, 220f, p)
        c.drawText("Time ${formatTime(time)}", 42f, 300f, p)
        val gp = Paint().apply { color = Color.rgb(0, 180, 0); strokeWidth = 4f }
        c.drawLine(30f, 560f, 260f, 560f, gp)
        c.drawLine(260f, 560f, 360f, 500f, gp)
        c.drawLine(360f, 500f, 650f, 500f, gp)

        val dir = File(cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "share_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(this, "com.juga.platform.fileprovider", file)
    }
}
