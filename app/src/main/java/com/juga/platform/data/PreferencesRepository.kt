package com.juga.platform.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesRepository(context: Context) {
    private val store = appDataStore(context)

    val showPad: Flow<Boolean> = store.data.map { it[KEY_SHOW_PAD] ?: true }
    val showGhost: Flow<Boolean> = store.data.map { it[KEY_SHOW_GHOST] ?: true }
    val checkpointsEnabled: Flow<Boolean> = store.data.map { it[KEY_CHECKPOINTS] ?: true }
    val dustEnabled: Flow<Boolean> = store.data.map { it[KEY_DUST] ?: true }
    val highestUnlocked: Flow<Int> = store.data.map { it[KEY_UNLOCKED] ?: 1 }

    suspend fun setShowPad(v: Boolean) = store.edit { it[KEY_SHOW_PAD] = v }
    suspend fun setShowGhost(v: Boolean) = store.edit { it[KEY_SHOW_GHOST] = v }
    suspend fun setCheckpointsEnabled(v: Boolean) = store.edit { it[KEY_CHECKPOINTS] = v }
    suspend fun setDustEnabled(v: Boolean) = store.edit { it[KEY_DUST] = v }
    suspend fun setHighestUnlocked(level: Int) = store.edit { prefs ->
        val current = prefs[KEY_UNLOCKED] ?: 1
        if (level > current) prefs[KEY_UNLOCKED] = level
    }

    fun bestTime(levelId: Int): Flow<Float?> = store.data.map { it[floatPreferencesKey("best_$levelId")] }

    suspend fun saveBestTime(levelId: Int, seconds: Float): Boolean {
        var improved = false
        store.edit {
            val key = floatPreferencesKey("best_$levelId")
            val existing = it[key]
            if (existing == null || seconds < existing) {
                it[key] = seconds
                improved = true
            }
        }
        return improved
    }

    companion object {
        private val KEY_SHOW_PAD = booleanPreferencesKey("show_pad")
        private val KEY_SHOW_GHOST = booleanPreferencesKey("show_ghost")
        private val KEY_CHECKPOINTS = booleanPreferencesKey("checkpoints")
        private val KEY_DUST = booleanPreferencesKey("dust")
        private val KEY_UNLOCKED = intPreferencesKey("unlocked")
    }
}
