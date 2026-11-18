package com.shoestringresearch.spamscreener.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "screening_settings")

data class ScreeningSettings(
  val isScreeningEnabled: Boolean,
  val screeningPatterns: List<String>
)

class ScreeningPreferencesRepository(private val context: Context) {
  companion object {
    private val KEY_SCREENING_ENABLED = booleanPreferencesKey("screening_enabled")
    private val KEY_SCREENING_PATTERNS = stringPreferencesKey("screening_patterns_json")
  }

  val settingsFlow: Flow<ScreeningSettings> = context.dataStore.data.map { preferences ->
    val enabled = preferences[KEY_SCREENING_ENABLED] ?: false
    val patternsJson = preferences[KEY_SCREENING_PATTERNS] ?: "[]"
    val patterns = parseJsonArray(patternsJson)
    ScreeningSettings(
      isScreeningEnabled = enabled,
      screeningPatterns = patterns
    )
  }

  suspend fun getSettings(): ScreeningSettings {
    return settingsFlow.first()
  }

  suspend fun setScreeningEnabled(enabled: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[KEY_SCREENING_ENABLED] = enabled
    }
  }

  suspend fun updatePattern(index: Int, pattern: String) {
    context.dataStore.edit { preferences ->
      val currentList = parseJsonArray(preferences[KEY_SCREENING_PATTERNS] ?: "[]").toMutableList()
      if (index in currentList.indices) {
        currentList[index] = pattern
        preferences[KEY_SCREENING_PATTERNS] = toJsonArray(currentList)
      }
    }
  }

  suspend fun addPattern(pattern: String = "") {
    context.dataStore.edit { preferences ->
      val currentList = parseJsonArray(preferences[KEY_SCREENING_PATTERNS] ?: "[]").toMutableList()
      currentList.add(pattern)
      preferences[KEY_SCREENING_PATTERNS] = toJsonArray(currentList)
    }
  }

  suspend fun removePattern(index: Int) {
    context.dataStore.edit { preferences ->
      val currentList = parseJsonArray(preferences[KEY_SCREENING_PATTERNS] ?: "[]").toMutableList()
      if (index in currentList.indices) {
        currentList.removeAt(index)
        preferences[KEY_SCREENING_PATTERNS] = toJsonArray(currentList)
      }
    }
  }

  suspend fun setPatterns(patterns: List<String>) {
    context.dataStore.edit { preferences ->
      preferences[KEY_SCREENING_PATTERNS] = toJsonArray(patterns)
    }
  }

  private fun parseJsonArray(jsonString: String): List<String> {
    return try {
      val jsonArray = JSONArray(jsonString)
      val list = mutableListOf<String>()
      for (i in 0 until jsonArray.length()) {
        list.add(jsonArray.getString(i))
      }
      list
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun toJsonArray(list: List<String>): String {
    val jsonArray = JSONArray()
    for (item in list) {
      jsonArray.put(item)
    }
    return jsonArray.toString()
  }
}
