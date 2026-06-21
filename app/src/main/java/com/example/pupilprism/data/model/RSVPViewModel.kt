package com.example.pupilprism.data.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pupilprism.data.db.AssessmentDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RSVPViewModel(
    private val assessmentDao: AssessmentDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(RSVPState())
    val uiState: StateFlow<RSVPState> = _uiState.asStateFlow()

    private var readingJob: Job? = null

    // --- Telemetry Data ---
    private var sessionStartTime: Long = 0L
    private var lastResumeTime: Long = 0L

    var totalReadingTimeMs: Long = 0L
        private set
    var backtrackCount: Int = 0
        private set
    var initialWpm: Int = 0
        private set

    private var materialId: String = ""

    fun loadContent(text: String, id: String, startWpm: Int, isCalibration: Boolean, startIndex: Int = 0) {
        val extractedWords = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        materialId = id
        initialWpm = startWpm

        _uiState.update {
            it.copy(
                words = extractedWords,
                // Ensure the start index doesn't exceed the bounds of the book
                currentIndex = startIndex.coerceIn(0, maxOf(0, extractedWords.size - 1)),
                wpm = startWpm,
                isPaused = true,
                isFinished = false,
                isCalibrationMode = isCalibration
            )
        }

        // Reset telemetry
        totalReadingTimeMs = 0L
        backtrackCount = 0
        sessionStartTime = System.currentTimeMillis()
    }

    fun togglePause() {
        val currentState = _uiState.value
        if (currentState.isFinished || currentState.words.isEmpty()) return

        val willPause = !currentState.isPaused
        _uiState.update { it.copy(isPaused = willPause) }

        if (willPause) {
            stopReadingLoop()
        } else {
            startReadingLoop()
        }
    }

    private fun startReadingLoop() {
        lastResumeTime = System.currentTimeMillis()
        readingJob?.cancel()

        readingJob = viewModelScope.launch {
            while (isActive && !_uiState.value.isPaused) {
                val state = _uiState.value

                // Check if we reached the end
                if (state.currentIndex >= state.words.size - 1) {
                    finishReading()
                    break
                }

                // 1. Calculate the delay based on WPM and Adaptive settings
                val delayTime = calculateDelay(state.words[state.currentIndex], state.wpm, state.isAdaptiveSpeedEnabled)

                // 2. Wait
                delay(delayTime)

                // 3. Move to next word
                _uiState.update { it.copy(currentIndex = it.currentIndex + 1) }
            }
        }
    }

    private fun stopReadingLoop() {
        readingJob?.cancel()
        if (lastResumeTime > 0) {
            totalReadingTimeMs += (System.currentTimeMillis() - lastResumeTime)
            lastResumeTime = 0L
        }
    }

    private fun finishReading() {
        stopReadingLoop()
        _uiState.update { it.copy(isFinished = true, isPaused = true) }

        // At this point, the text is done. The UI can observe `isFinished == true`
        // and navigate to the Comprehension Quiz screen.
    }

    // --- User Controls & Telemetry Tracking ---

    fun changeWpm(delta: Int) {
        _uiState.update {
            val newWpm = (it.wpm + delta).coerceAtLeast(50)
            it.copy(wpm = newWpm)
        }
    }

    fun toggleAdaptiveSpeed() {
        _uiState.update { it.copy(isAdaptiveSpeedEnabled = !it.isAdaptiveSpeedEnabled) }
    }

    fun backtrack() {
        _uiState.update {
            if (it.currentIndex > 0) {
                backtrackCount++ // Record the telemetry event
                it.copy(currentIndex = it.currentIndex - 1)
            } else {
                it
            }
        }
    }

    fun forward() {
        _uiState.update {
            if (it.currentIndex < it.words.size - 1) {
                it.copy(currentIndex = it.currentIndex + 1)
            } else {
                it
            }
        }
    }

    // --- Core Algorithm ---

    private fun calculateDelay(currentWord: String, currentWpm: Int, isAdaptive: Boolean): Long {
        val baseDelayMillis = (60000L / currentWpm)

        var finalDelay = if (isAdaptive) {
            val lengthRatio = currentWord.length / 5.0 // Assuming 5 chars is average
            (baseDelayMillis * 0.5 + baseDelayMillis * 0.5 * lengthRatio).toLong()
        } else {
            baseDelayMillis
        }

        if (isAdaptive && currentWord.isNotEmpty()) {
            val lastChar = currentWord.last()
            finalDelay = when (lastChar) {
                '.', '!', '?' -> (finalDelay * 2.0).toLong() // 100% extra delay
                ',', ';', ':' -> (finalDelay * 1.5).toLong() // 50% extra delay
                else -> finalDelay
            }
        }
        return finalDelay
    }

    override fun onCleared() {
        super.onCleared()
        stopReadingLoop()
    }

    fun loadMaterialFromDb(materialId: String, startWpm: Int, isCalibration: Boolean) {
        viewModelScope.launch {
            val material = assessmentDao.getMaterialById(materialId)
            if (material != null) {
                // Uses your existing loadContent function
                loadContent(material.content, materialId, startWpm, isCalibration)
            }
        }
    }
}