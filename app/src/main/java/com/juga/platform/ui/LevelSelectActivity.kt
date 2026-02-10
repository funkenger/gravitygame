package com.juga.platform.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.juga.platform.R
import com.juga.platform.data.GhostRepository
import com.juga.platform.data.LevelRepository
import com.juga.platform.data.PreferencesRepository
import com.juga.platform.model.Level
import com.juga.platform.model.MedalType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LevelSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPortrait()
        setContentView(R.layout.activity_level_select)

        val recycler = findViewById<RecyclerView>(R.id.recyclerLevels)
        recycler.layoutManager = GridLayoutManager(this, 2)

        lifecycleScope.launch {
            val levels = LevelRepository(this@LevelSelectActivity).loadAllLevels()
            val prefs = PreferencesRepository(this@LevelSelectActivity)
            val ghostRepo = GhostRepository(this@LevelSelectActivity)
            val unlocked = prefs.highestUnlocked.first()
            val rows = levels.map { level ->
                val best = prefs.bestTime(level.id).first()
                val medal = medalFor(level, best)
                LevelRow(level, level.id <= unlocked, best, medal, ghostRepo.hasGhost(level.id))
            }
            recycler.adapter = LevelAdapter(rows) { id ->
                startActivity(Intent(this@LevelSelectActivity, GameActivity::class.java).putExtra("level", id))
            }
        }
    }

    private fun medalFor(level: Level, best: Float?): MedalType {
        if (best == null) return MedalType.NONE
        return when {
            best <= level.medals.gold -> MedalType.GOLD
            best <= level.medals.silver -> MedalType.SILVER
            best <= level.medals.bronze -> MedalType.BRONZE
            else -> MedalType.NONE
        }
    }
}

data class LevelRow(
    val level: Level,
    val unlocked: Boolean,
    val best: Float?,
    val medal: MedalType,
    val hasGhost: Boolean
)

class LevelAdapter(
    private val rows: List<LevelRow>,
    private val onTap: (Int) -> Unit
) : RecyclerView.Adapter<LevelAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.txtName)
        val status: TextView = v.findViewById(R.id.txtStatus)
        val best: TextView = v.findViewById(R.id.txtBest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_level, parent, false)
        return Holder(v)
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        holder.name.text = "${row.level.id}. ${row.level.name}"
        holder.status.text = if (row.unlocked) "Unlocked • ${row.medal}" else "Locked"
        holder.best.text = "Best: ${formatTime(row.best)} • Ghost: ${if (row.hasGhost) "Yes" else "No"}"
        holder.itemView.alpha = if (row.unlocked) 1f else 0.5f
        holder.itemView.setOnClickListener { if (row.unlocked) onTap(row.level.id) }
    }
}
