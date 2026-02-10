package com.juga.platform.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class GhostSample(
    val t: Float,
    val cx: Float,
    val cy: Float,
    val ca: Float,
    val rx: Float,
    val ry: Float,
    val fx: Float,
    val fy: Float
)

class GhostStore(private val context: Context) {
    private fun file(levelId: Int) = File(context.filesDir, "ghost_$levelId.json")

    fun hasGhost(levelId: Int): Boolean = file(levelId).exists()

    fun save(levelId: Int, samples: List<GhostSample>) {
        val arr = JSONArray()
        samples.forEach {
            arr.put(JSONObject()
                .put("t", it.t)
                .put("cx", it.cx)
                .put("cy", it.cy)
                .put("ca", it.ca)
                .put("rx", it.rx)
                .put("ry", it.ry)
                .put("fx", it.fx)
                .put("fy", it.fy)
            )
        }
        file(levelId).writeText(arr.toString())
    }

    fun load(levelId: Int): List<GhostSample> {
        val f = file(levelId)
        if (!f.exists()) return emptyList()
        val arr = JSONArray(f.readText())
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            GhostSample(
                t = o.getDouble("t").toFloat(),
                cx = o.getDouble("cx").toFloat(),
                cy = o.getDouble("cy").toFloat(),
                ca = o.getDouble("ca").toFloat(),
                rx = o.getDouble("rx").toFloat(),
                ry = o.getDouble("ry").toFloat(),
                fx = o.getDouble("fx").toFloat(),
                fy = o.getDouble("fy").toFloat()
            )
        }
    }
}
