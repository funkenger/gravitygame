package com.juga.platform.data

import android.content.Context
import com.juga.platform.physics.BikeSnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class GhostRepository(private val context: Context) {
    private fun fileFor(levelId: Int): File = File(context.filesDir, "ghost_$levelId.json")

    fun save(levelId: Int, data: List<BikeSnapshot>) {
        val arr = JSONArray()
        data.forEach {
            arr.put(
                JSONObject()
                    .put("t", it.t)
                    .put("fx", it.frameX)
                    .put("fy", it.frameY)
                    .put("fa", it.frameA)
                    .put("bx", it.backX)
                    .put("by", it.backY)
                    .put("nx", it.frontX)
                    .put("ny", it.frontY)
            )
        }
        fileFor(levelId).writeText(arr.toString())
    }

    fun load(levelId: Int): List<BikeSnapshot> {
        val f = fileFor(levelId)
        if (!f.exists()) return emptyList()
        val arr = JSONArray(f.readText())
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            BikeSnapshot(
                t = o.getDouble("t").toFloat(),
                frameX = o.getDouble("fx").toFloat(),
                frameY = o.getDouble("fy").toFloat(),
                frameA = o.getDouble("fa").toFloat(),
                backX = o.getDouble("bx").toFloat(),
                backY = o.getDouble("by").toFloat(),
                frontX = o.getDouble("nx").toFloat(),
                frontY = o.getDouble("ny").toFloat()
            )
        }
    }

    fun hasGhost(levelId: Int): Boolean = fileFor(levelId).exists()
}
