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
import com.juga.platform.data.GhostStore
import com.juga.platform.data.LevelData
import com.juga.platform.data.LevelLoader
import com.juga.platform.data.Medal
import com.juga.platform.data.RecordsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LevelSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPortrait()
        setContentView(R.layout.activity_level_select)
        val list = findViewById<RecyclerView>(R.id.recyclerLevels)
        list.layoutManager = GridLayoutManager(this, 2)

        lifecycleScope.launch {
            val levels = LevelLoader(this@LevelSelectActivity).loadAll()
            val records = RecordsStore(this@LevelSelectActivity)
            val ghostStore = GhostStore(this@LevelSelectActivity)
            val unlocked = records.highestUnlocked.first()
            val rows = levels.map { l ->
                val best = records.bestTime(l.id).first()
                val medal = when {
                    best == null -> Medal.NONE
                    best <= l.medals.gold -> Medal.GOLD
                    best <= l.medals.silver -> Medal.SILVER
                    best <= l.medals.bronze -> Medal.BRONZE
                    else -> Medal.NONE
                }
                Row(l, l.id <= unlocked, best, medal, ghostStore.hasGhost(l.id))
            }
            list.adapter = Adapter(rows) {
                startActivity(Intent(this@LevelSelectActivity, GameActivity::class.java).putExtra("level", it))
            }
        }
    }

    private data class Row(val level: LevelData, val unlocked: Boolean, val best: Float?, val medal: Medal, val hasGhost: Boolean)

    private class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.txtName)
        val status: TextView = v.findViewById(R.id.txtStatus)
        val best: TextView = v.findViewById(R.id.txtBest)
    }

    private class Adapter(private val rows: List<Row>, private val onTap: (Int) -> Unit) : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_level, parent, false)
            return VH(v)
        }

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val r = rows[position]
            holder.name.text = "Level ${r.level.id}"
            holder.status.text = if (r.unlocked) "${r.medal} • Ghost:${if (r.hasGhost) "Yes" else "No"}" else "Locked"
            holder.best.text = "Best: ${formatTime(r.best)}"
            holder.itemView.alpha = if (r.unlocked) 1f else 0.45f
            holder.itemView.setOnClickListener { if (r.unlocked) onTap(r.level.id) }
        }
    }
}
