package com.juga.platform.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.juga.platform.R
import com.juga.platform.data.RecordsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TitleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPortrait()
        setContentView(R.layout.activity_main)

        val records = RecordsStore(this)
        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            lifecycleScope.launch {
                val lvl = records.highestUnlocked.first()
                startActivity(Intent(this@TitleActivity, GameActivity::class.java).putExtra("level", lvl))
            }
        }
        findViewById<Button>(R.id.btnLevels).setOnClickListener { startActivity(Intent(this, LevelSelectActivity::class.java)) }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }
}
