package com.juga.platform.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recordsDataStore: DataStore<Preferences> by preferencesDataStore(name = "juga_records")

class RecordsStore(context: Context) {
    private val store = context.applicationContext.recordsDataStore

    val highestUnlocked: Flow<Int> = store.data.map { it[intPreferencesKey("unlocked")] ?: 1 }
    val ghostEnabled: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("ghost")] ?: true }
    val checkpointsEnabled: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("checkpoints")] ?: true }
    val dPadVisible: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("dpad")] ?: true }
    val hapticsEnabled: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("haptics")] ?: true }
    val soundEnabled: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("sound")] ?: false }
    val debugOverlayEnabled: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("debug_overlay")] ?: true }
    val verticalCenteringEnabled: Flow<Boolean> = store.data.map { it[booleanPreferencesKey("vertical_centering")] ?: true }

    fun bestTime(level: Int): Flow<Float?> = store.data.map { it[floatPreferencesKey("best_$level")] }

    suspend fun setToggle(key: String, value: Boolean) = store.edit {
        it[booleanPreferencesKey(key)] = value
    }

    suspend fun saveTime(level: Int, sec: Float): Boolean {
        var improved = false
        store.edit {
            val k = floatPreferencesKey("best_$level")
            val old = it[k]
            if (old == null || sec < old) {
                it[k] = sec
                improved = true
            }
            it[floatPreferencesKey("last_$level")] = sec
        }
        return improved
    }

    suspend fun unlockNext(level: Int, maxLevel: Int) = store.edit {
        val k = intPreferencesKey("unlocked")
        val next = (level + 1).coerceAtMost(maxLevel)
        val old = it[k] ?: 1
        if (next > old) it[k] = next
    }
}
