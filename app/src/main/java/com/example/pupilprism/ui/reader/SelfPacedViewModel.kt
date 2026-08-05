package com.example.pupilprism.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pupilprism.data.db.AssessmentDao
import com.example.pupilprism.data.db.UserStatsDao
import com.example.pupilprism.data.model.ReadingMaterial
import com.example.pupilprism.data.model.ReportData
import com.example.pupilprism.data.model.SessionTelemetryStore
import com.example.pupilprism.data.repository.ReportBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SelfPacedFlowState(
    val isLoading: Boolean = true,
    val currentStageIndex: Int = 0,
    val materials: List<ReadingMaterial> = emptyList(),
    val startWpm: Int = 250,
    val isComplete: Boolean = false
)

class SelfPacedViewModel(
    private val assessmentDao: AssessmentDao,
    private val userStatsDao: UserStatsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelfPacedFlowState())
    val uiState = _uiState.asStateFlow()

    private val _report = MutableStateFlow<ReportData?>(null)
    val report: StateFlow<ReportData?> = _report.asStateFlow()

    init {
        SessionTelemetryStore.condition = "self_paced"

        viewModelScope.launch {
            val optimal = userStatsDao.getStats()?.optimalWpm ?: 250
            assessmentDao.getMaterialsByMode(isCalibration = false).collect { materials ->
                if (materials.isNotEmpty()) {
                    _uiState.update {
                        it.copy(materials = materials, startWpm = optimal, isLoading = false)
                    }
                }
            }
        }
    }

    fun onTextFinished() {
        val next = _uiState.value.currentStageIndex + 1
        if (next < _uiState.value.materials.size) {
            _uiState.update { it.copy(currentStageIndex = next) }
        } else {
            finishAndBuildReport()
        }
    }

    private fun finishAndBuildReport() {
        _uiState.update { it.copy(isComplete = true) }
        viewModelScope.launch {
            val optimal = userStatsDao.getStats()?.optimalWpm ?: 250
            _report.value = ReportBuilder.build(assessmentDao, SessionTelemetryStore.runId, optimal)
        }
    }
}