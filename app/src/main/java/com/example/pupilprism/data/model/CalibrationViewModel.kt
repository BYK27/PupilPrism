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

    // CHANGED: The predefined testing speeds expanded to 8 stages
    val targetSpeeds = listOf(50, 100, 150, 200, 250, 300, 350, 400)
    private val stageResults = mutableListOf<StageResult>()

    init {
        loadCalibrationMaterials()
    }

    private fun loadCalibrationMaterials() {
        viewModelScope.launch {
            // Collect the flow reactively
            assessmentDao.getMaterialsByMode(isCalibration = true).collect { materials ->
                if (materials.isNotEmpty()) {
                    // CHANGED: Ensure we have enough materials for all 8 speeds by looping through available ones.
                    // This prevents crashes if your JSON seed file has fewer than 8 items.
                    val expandedMaterials = List(targetSpeeds.size) { index ->
                        materials[index % materials.size]
                    }

                    _uiState.update {
                        it.copy(
                            calibrationMaterials = expandedMaterials,
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
            // All 8 stages are complete. Run the algorithm.
            executeCalibrationAlgorithm()
        }
    }

    /**
     * THE CORE ALGORITHM:
     * Calculates the maximum WPM where the user maintained >= 80% comprehension.
     */
    private fun executeCalibrationAlgorithm() {
        val acceptablePerformances = stageResults.filter { it.accuracy >= 0.80f }

        val optimal = if (acceptablePerformances.isNotEmpty()) {
            acceptablePerformances.maxOf { it.wpmUsed }
        } else {
            50
        }

        viewModelScope.launch {
            val currentStats = userStatsDao.getStats() ?: UserStats()
            userStatsDao.insertOrUpdate(currentStats.copy(optimalWpm = optimal))

            _uiState.update {
                it.copy(
                    isCalibrationComplete = true,
                    finalCalculatedWpm = optimal
                )
            }
        }
    }
}