package com.juga.platform.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.juga.platform.R
import com.juga.platform.data.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPortrait()
        setContentView(R.layout.activity_main)
        prefs = PreferencesRepository(this)

        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            lifecycleScope.launch {
                val unlocked = prefs.highestUnlocked.first()
                startActivity(Intent(this@MainActivity, GameActivity::class.java).putExtra("level", unlocked))
            }
        }
        findViewById<Button>(R.id.btnLevels).setOnClickListener {
            startActivity(Intent(this, LevelSelectActivity::class.java))
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
