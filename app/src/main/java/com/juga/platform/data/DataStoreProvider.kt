package com.juga.platform.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.jugaDataStore: DataStore<Preferences> by preferencesDataStore(name = "juga_prefs")

internal fun appDataStore(context: Context): DataStore<Preferences> = context.applicationContext.jugaDataStore
