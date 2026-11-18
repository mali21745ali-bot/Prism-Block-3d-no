package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.audioDataStore by preferencesDataStore(name = "prism_audio_prefs")

data class AudioPreferences(
    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true
)

class PreferencesRepository(private val context: Context) {
    private val musicKey = booleanPreferencesKey("music_enabled")
    private val sfxKey = booleanPreferencesKey("sfx_enabled")

    val audioPreferences: Flow<AudioPreferences> = context.audioDataStore.data.map { prefs ->
        AudioPreferences(
            musicEnabled = prefs[musicKey] ?: true,
            sfxEnabled = prefs[sfxKey] ?: true
        )
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.audioDataStore.edit { it[musicKey] = enabled }
    }

    suspend fun setSfxEnabled(enabled: Boolean) {
        context.audioDataStore.edit { it[sfxKey] = enabled }
    }
}
