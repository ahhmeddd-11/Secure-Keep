package com.example.securekeep.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_settings")

class SecurityManager(private val context: Context) {
    companion object {
        private val APP_PIN = stringPreferencesKey("app_pin")
        private val NOTE_PIN = stringPreferencesKey("note_pin")
        private val USE_BIOMETRIC_APP = booleanPreferencesKey("use_biometric_app")
        private val USE_BIOMETRIC_NOTE = booleanPreferencesKey("use_biometric_note")
        private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }

    val appPin: Flow<String?> = context.dataStore.data.map { it[APP_PIN] }
    val notePin: Flow<String?> = context.dataStore.data.map { it[NOTE_PIN] }
    val useBiometricApp: Flow<Boolean> = context.dataStore.data.map { it[USE_BIOMETRIC_APP] ?: false }
    val useBiometricNote: Flow<Boolean> = context.dataStore.data.map { it[USE_BIOMETRIC_NOTE] ?: false }
    val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { it[IS_DARK_MODE] }

    suspend fun saveAppPin(pin: String) {
        context.dataStore.edit { it[APP_PIN] = pin }
    }

    suspend fun saveNotePin(pin: String) {
        context.dataStore.edit { it[NOTE_PIN] = pin }
    }

    suspend fun setUseBiometricApp(enabled: Boolean) {
        context.dataStore.edit { it[USE_BIOMETRIC_APP] = enabled }
    }

    suspend fun setUseBiometricNote(enabled: Boolean) {
        context.dataStore.edit { it[USE_BIOMETRIC_NOTE] = enabled }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[IS_DARK_MODE] = enabled }
    }
    
    suspend fun clearNotePin() {
        context.dataStore.edit { it.remove(NOTE_PIN) }
    }
}
