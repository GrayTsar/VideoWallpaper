package com.graytsar.livewallpaper.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext val context: Context) :
    IUserPreferences {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

    companion object {
        val FORCE_DARK_MODE = booleanPreferencesKey("pref_force_dark_mode")
    }

    override suspend fun saveForceDarkMode(b: Boolean) {
        context.dataStore.edit {
            it[FORCE_DARK_MODE] = b
        }
    }

    override suspend fun readForceDarkMode() = context.dataStore.data.map {
        it[FORCE_DARK_MODE] ?: false
    }
}