package com.example.anitama

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Tạo DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    // Key cho ngôn ngữ
    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val IS_LANGUAGE_SET_KEY = booleanPreferencesKey("is_language_set")
    }

    // Lưu ngôn ngữ
    suspend fun saveLanguage(language: String) {
        println("saveLanguage : $language")
        context.dataStore.edit { settings ->
            settings[LANGUAGE_KEY] = language
            settings[IS_LANGUAGE_SET_KEY] = true
        }
    }

    // Đọc ngôn ngữ
    val languageFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE_KEY] ?: "en" // Giá trị mặc định là "en"
        }

    // Đọc trạng thái đã chọn ngôn ngữ
    val isLanguageSetFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_LANGUAGE_SET_KEY] ?: false // Mặc định là false nếu chưa chọn
        }
}