package com.juga.platform.ui

import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.juga.platform.R
import com.juga.platform.data.RecordsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPortrait()
        setContentView(R.layout.activity_settings)

        val store = RecordsStore(this)
        val cbPad = findViewById<CheckBox>(R.id.cbPad)
        val cbGhost = findViewById<CheckBox>(R.id.cbGhost)
        val cbCheck = findViewById<CheckBox>(R.id.cbCheckpoints)
        val cbHaptics = findViewById<CheckBox>(R.id.cbHaptics)
        val cbSound = findViewById<CheckBox>(R.id.cbSound)
        val cbCentering = findViewById<CheckBox>(R.id.cbVerticalCentering)
        val cbDebug = findViewById<CheckBox>(R.id.cbDebugOverlay)

        lifecycleScope.launch {
            cbPad.isChecked = store.dPadVisible.first()
            cbGhost.isChecked = store.ghostEnabled.first()
            cbCheck.isChecked = store.checkpointsEnabled.first()
            cbHaptics.isChecked = store.hapticsEnabled.first()
            cbSound.isChecked = store.soundEnabled.first()
            cbCentering.isChecked = store.verticalCenteringEnabled.first()
            cbDebug.isChecked = store.debugOverlayEnabled.first()
        }

        cbPad.setOnCheckedChangeListener { _, b -> lifecycleScope.launch { store.setToggle("dpad", b) } }
        cbGhost.setOnCheckedChangeListener { _, b -> lifecycleScope.launch { store.setToggle("ghost", b) } }
        cbCheck.setOnCheckedChangeListener { _, b -> lifecycleScope.launch { store.setToggle("checkpoints", b) } }
        cbHaptics.setOnCheckedChangeListener { _, b -> lifecycleScope.launch { store.setToggle("haptics", b) } }
        cbSound.setOnCheckedChangeListener { _, b -> lifecycleScope.launch { store.setToggle("sound", b) } }
        cbCentering.setOnCheckedChangeListener { _, b -> lifecycleScope.launch { store.setToggle("vertical_centering", b) } }
        cbDebug.setOnCheckedChangeListener { _, b -> lifecycleScope.launch { store.setToggle("debug_overlay", b) } }
    }
}
