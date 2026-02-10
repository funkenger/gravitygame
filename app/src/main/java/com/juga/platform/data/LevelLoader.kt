package com.juga.platform.data

import android.content.Context
import org.json.JSONObject

class LevelLoader(private val context: Context) {
    fun loadAll(): List<LevelData> {
        val files = context.assets.list("levels")?.sorted().orEmpty()
        return files.map { file ->
            val txt = context.assets.open("levels/$file").bufferedReader().use { it.readText() }
            parse(JSONObject(txt))
        }.sortedBy { it.id }
    }

    fun level(id: Int): LevelData = loadAll().first { it.id == id }

    private fun parse(j: JSONObject): LevelData {
        val s = j.getJSONObject("start")
        val m = j.getJSONObject("medals")
        val segJson = j.getJSONArray("segments")
        val segments = MutableList(segJson.length()) { idx ->
            val x = segJson.getJSONObject(idx)
            TrackSegment(
                x.getDouble("x1").toFloat(),
                x.getDouble("y1").toFloat(),
                x.getDouble("x2").toFloat(),
                x.getDouble("y2").toFloat()
            )
        }
        val checkpoints = mutableListOf<VecPoint>()
        if (j.has("checkpoints")) {
            val cps = j.getJSONArray("checkpoints")
            repeat(cps.length()) { i ->
                val p = cps.getJSONObject(i)
                checkpoints += VecPoint(p.getDouble("x").toFloat(), p.getDouble("y").toFloat())
            }
        }
        return LevelData(
            id = j.getInt("id"),
            name = j.getString("name"),
            start = VecPoint(s.getDouble("x").toFloat(), s.getDouble("y").toFloat()),
            finishX = j.getDouble("finishX").toFloat(),
            medals = MedalThresholds(
                gold = m.getDouble("gold").toFloat(),
                silver = m.getDouble("silver").toFloat(),
                bronze = m.getDouble("bronze").toFloat()
            ),
            segments = segments,
            checkpoints = checkpoints
        )
    }
}
