package com.juga.platform.data

import android.content.Context
import com.juga.platform.model.Level
import com.juga.platform.model.Medals
import com.juga.platform.model.Point
import com.juga.platform.model.Segment
import org.json.JSONObject

class LevelRepository(private val context: Context) {
    fun loadAllLevels(): List<Level> {
        val levels = mutableListOf<Level>()
        val files = context.assets.list("levels")?.sorted().orEmpty()
        for (file in files) {
            val content = context.assets.open("levels/$file").bufferedReader().use { it.readText() }
            levels += parseLevel(JSONObject(content))
        }
        return levels.sortedBy { it.id }
    }

    private fun parseLevel(json: JSONObject): Level {
        val start = json.getJSONObject("start")
        val medals = json.getJSONObject("medals")
        val segmentArray = json.getJSONArray("segments")
        val segments = List(segmentArray.length()) { idx ->
            val s = segmentArray.getJSONObject(idx)
            Segment(
                x1 = s.getDouble("x1").toFloat(),
                y1 = s.getDouble("y1").toFloat(),
                x2 = s.getDouble("x2").toFloat(),
                y2 = s.getDouble("y2").toFloat()
            )
        }
        val checkpoints = mutableListOf<Point>()
        if (json.has("checkpoints")) {
            val cp = json.getJSONArray("checkpoints")
            repeat(cp.length()) { i ->
                val p = cp.getJSONObject(i)
                checkpoints += Point(p.getDouble("x").toFloat(), p.getDouble("y").toFloat())
            }
        }

        return Level(
            id = json.getInt("id"),
            name = json.getString("name"),
            start = Point(start.getDouble("x").toFloat(), start.getDouble("y").toFloat()),
            finishX = json.getDouble("finishX").toFloat(),
            medals = Medals(
                medals.getDouble("gold").toFloat(),
                medals.getDouble("silver").toFloat(),
                medals.getDouble("bronze").toFloat()
            ),
            segments = segments,
            checkpoints = checkpoints
        )
    }
}
