package com.example.pupilprism.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pupilprism.data.db.AssessmentDao
import com.example.pupilprism.data.db.UserStatsDao
import com.example.pupilprism.data.model.ReadingMaterial
import com.example.pupilprism.data.model.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Internal data class to hold the results of each stage
private data class StageResult(val wpmUsed: Int, val accuracy: Float)

data class CalibrationFlowState(
    val isLoading: Boolean = true,
    val currentStageIndex: Int = 0,
    val calibrationMaterials: List<ReadingMaterial> = emptyList(),
    val isCalibrationComplete: Boolean = false,
    val finalCalculatedWpm: Int? = null
)

class CalibrationViewModel(
    private val assessmentDao: AssessmentDao,
    private val userStatsDao: UserStatsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalibrationFlowState())
    val uiState = _uiState.asStateFlow()

    // The predefined testing speeds
    val targetSpeeds = listOf(250, 350, 450)
    private val stageResults = mutableListOf<StageResult>()

    init {
        loadCalibrationMaterials()
    }

    private fun loadCalibrationMaterials() {
        viewModelScope.launch {
            // Collect the flow reactively
            assessmentDao.getMaterialsByMode(isCalibration = true).collect { materials ->
                if (materials.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            calibrationMaterials = materials.take(3),
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    /**
     * Called by the AssessmentQuizScreen after the user finishes the quiz for a specific text.
     */
    fun processStageResult(correctAnswers: Int, totalQuestions: Int) {
        val accuracy = if (totalQuestions > 0) correctAnswers.toFloat() / totalQuestions else 0f
        val currentSpeed = targetSpeeds[_uiState.value.currentStageIndex]

        stageResults.add(StageResult(currentSpeed, accuracy))

        val nextStage = _uiState.value.currentStageIndex + 1

        if (nextStage < targetSpeeds.size && nextStage < _uiState.value.calibrationMaterials.size) {
            // Move to the next text and speed
            _uiState.update { it.copy(currentStageIndex = nextStage) }
        } else {
            // All 3 stages are complete. Run the algorithm.
            executeCalibrationAlgorithm()
        }
    }

    /**
     * THE CORE ALGORITHM:
     * Calculates the maximum WPM where the user maintained >= 80% comprehension.
     */
    private fun executeCalibrationAlgorithm() {
        // 1. Filter out any stages where comprehension dropped below 80%
        val acceptablePerformances = stageResults.filter { it.accuracy >= 0.80f }

        // 2. Find the highest speed from the acceptable performances, default to 200 if all failed
        val optimal = if (acceptablePerformances.isNotEmpty()) {
            acceptablePerformances.maxOf { it.wpmUsed }
        } else {
            200 // Fallback baseline
        }

        // 3. Save to UserStats
        viewModelScope.launch {
            val currentStats = userStatsDao.getStats() ?: UserStats()
            userStatsDao.insertOrUpdate(currentStats.copy(optimalWpm = optimal))

            // 4. Trigger UI navigation/completion
            _uiState.update {
                it.copy(
                    isCalibrationComplete = true,
                    finalCalculatedWpm = optimal
                )
            }
        }
    }
}