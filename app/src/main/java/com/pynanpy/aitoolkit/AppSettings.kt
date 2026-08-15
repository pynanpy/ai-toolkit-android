package com.pynanpy.aitoolkit

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "ai_toolkit_settings"
)

data class AppSettings(
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini"
)

object SettingsRepository {

    private val BASE_URL = stringPreferencesKey("base_url")
    private val API_KEY = stringPreferencesKey("api_key")
    private val MODEL = stringPreferencesKey("model")

    suspend fun load(context: Context): AppSettings {
        val preferences = context.dataStore.data.first()

        return AppSettings(
            baseUrl = preferences[BASE_URL]
                ?: "https://api.openai.com/v1",

            apiKey = preferences[API_KEY]
                ?: "",

            model = preferences[MODEL]
                ?: "gpt-4o-mini"
        )
    }

    suspend fun save(
        context: Context,
        settings: AppSettings
    ) {
        context.dataStore.edit { preferences ->

            preferences[BASE_URL] = settings.baseUrl
            preferences[API_KEY] = settings.apiKey
            preferences[MODEL] = settings.model
        }
    }
}