package com.shoestringresearch.spamscreener.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shoestringresearch.spamscreener.data.ScreeningPreferencesRepository
import com.shoestringresearch.spamscreener.util.RegexValidationResult
import com.shoestringresearch.spamscreener.util.RegexValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class PatternItemUiModel(
  val id: String = UUID.randomUUID().toString(),
  val pattern: String,
  val validationResult: RegexValidationResult
)

data class MainUiState(
  val isScreeningEnabled: Boolean = false,
  val patterns: List<PatternItemUiModel> = emptyList(),
  val isLoading: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = ScreeningPreferencesRepository(application)

  private val _uiState = MutableStateFlow(MainUiState())
  val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

  private var currentItemIds: List<String> = emptyList()
  private var savePatternsJob: Job? = null

  init {
    viewModelScope.launch {
      repository.settingsFlow.collect { settings ->
        val currentPatterns = _uiState.value.patterns.map { it.pattern }
        if (_uiState.value.isLoading || currentPatterns != settings.screeningPatterns) {
          val updatedIds = buildList {
            for (i in settings.screeningPatterns.indices) {
              if (i < currentItemIds.size) {
                add(currentItemIds[i])
              } else {
                add(UUID.randomUUID().toString())
              }
            }
          }
          currentItemIds = updatedIds

          _uiState.value = MainUiState(
            isScreeningEnabled = settings.isScreeningEnabled,
            patterns = settings.screeningPatterns.mapIndexed { index, pattern ->
              PatternItemUiModel(
                id = updatedIds[index],
                pattern = pattern,
                validationResult = RegexValidator.validate(pattern)
              )
            },
            isLoading = false
          )
        } else if (_uiState.value.isScreeningEnabled != settings.isScreeningEnabled) {
          _uiState.value = _uiState.value.copy(isScreeningEnabled = settings.isScreeningEnabled)
        }
      }
    }
  }

  fun onScreeningEnabledChanged(enabled: Boolean) {
    _uiState.value = _uiState.value.copy(isScreeningEnabled = enabled)
    viewModelScope.launch {
      repository.setScreeningEnabled(enabled)
    }
  }

  fun onAddPattern() {
    val newId = UUID.randomUUID().toString()
    currentItemIds = currentItemIds + newId
    val newPattern = PatternItemUiModel(
      id = newId,
      pattern = "",
      validationResult = RegexValidator.validate("")
    )
    val updatedPatterns = _uiState.value.patterns + newPattern
    _uiState.value = _uiState.value.copy(patterns = updatedPatterns)
    persistPatternsImmediately()
  }

  fun onUpdatePattern(index: Int, newPattern: String) {
    val currentPatterns = _uiState.value.patterns
    if (index in currentPatterns.indices) {
      val updatedPatterns = currentPatterns.toMutableList()
      updatedPatterns[index] = updatedPatterns[index].copy(
        pattern = newPattern,
        validationResult = RegexValidator.validate(newPattern)
      )
      _uiState.value = _uiState.value.copy(patterns = updatedPatterns)
      persistPatternsDebounced()
    }
  }

  fun onRemovePattern(index: Int) {
    if (index in currentItemIds.indices) {
      currentItemIds = currentItemIds.toMutableList().apply { removeAt(index) }
    }
    val currentPatterns = _uiState.value.patterns
    if (index in currentPatterns.indices) {
      val updatedPatterns = currentPatterns.toMutableList().apply { removeAt(index) }
      _uiState.value = _uiState.value.copy(patterns = updatedPatterns)
      persistPatternsImmediately()
    }
  }

  private fun persistPatternsDebounced() {
    savePatternsJob?.cancel()
    savePatternsJob = viewModelScope.launch {
      delay(300)
      val patterns = _uiState.value.patterns.map { it.pattern }
      repository.setPatterns(patterns)
    }
  }

  private fun persistPatternsImmediately() {
    savePatternsJob?.cancel()
    savePatternsJob = viewModelScope.launch {
      val patterns = _uiState.value.patterns.map { it.pattern }
      repository.setPatterns(patterns)
    }
  }
}

