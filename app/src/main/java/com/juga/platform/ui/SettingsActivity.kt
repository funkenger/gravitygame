package com.juga.platform.ui

import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.juga.platform.R
import com.juga.platform.data.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPortrait()
        setContentView(R.layout.activity_settings)

        val prefs = PreferencesRepository(this)
        val cbPad = findViewById<CheckBox>(R.id.cbPad)
        val cbGhost = findViewById<CheckBox>(R.id.cbGhost)
        val cbCheck = findViewById<CheckBox>(R.id.cbCheckpoints)
        val cbDust = findViewById<CheckBox>(R.id.cbDust)

        lifecycleScope.launch {
            cbPad.isChecked = prefs.showPad.first()
            cbGhost.isChecked = prefs.showGhost.first()
            cbCheck.isChecked = prefs.checkpointsEnabled.first()
            cbDust.isChecked = prefs.dustEnabled.first()
        }

        cbPad.setOnCheckedChangeListener { _, b -> lifecycleScope.launch { prefs.setShowPad(b) } }
        cbGhost.setOnCheckedChangeListener { _, b -> lifecycleScope.launch { prefs.setShowGhost(b) } }
        cbCheck.setOnCheckedChangeListener { _, b -> lifecycleScope.launch { prefs.setCheckpointsEnabled(b) } }
        cbDust.setOnCheckedChangeListener { _, b -> lifecycleScope.launch { prefs.setDustEnabled(b) } }
    }
}
