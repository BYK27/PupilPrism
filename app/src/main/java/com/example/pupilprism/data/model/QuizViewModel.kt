package com.example.pupilprism.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pupilprism.data.db.AssessmentDao
import com.example.pupilprism.data.model.AssessmentSession
import com.example.pupilprism.data.model.ComprehensionQuestion
import com.example.pupilprism.data.model.SessionTelemetryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuizViewModel(private val assessmentDao: AssessmentDao) : ViewModel() {

    private val _questions = MutableStateFlow<List<ComprehensionQuestion>>(emptyList())
    val questions: StateFlow<List<ComprehensionQuestion>> = _questions.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    var correctAnswersCount = 0
        private set

    fun loadQuestions(materialId: String) {
        viewModelScope.launch {
            _questions.value = assessmentDao.getQuestionsForMaterial(materialId)
            _currentIndex.value = 0
            correctAnswersCount = 0
        }
    }

    /** Враћа true ако је квиз завршен. */
    fun submitAnswer(selectedIndex: Int): Boolean {
        val pitanje = _questions.value.getOrNull(_currentIndex.value) ?: return true

        if (selectedIndex == pitanje.correctAnswerIndex) correctAnswersCount++

        val gotovo = _currentIndex.value >= _questions.value.size - 1
        return if (gotovo) {
            saveSessionResults()
            true
        } else {
            _currentIndex.value += 1
            false
        }
    }

    private fun saveSessionResults() {
        // Ако нема телеметрије, читање је било ван испитивања. Не уписујемо ништа.
        val p = SessionTelemetryStore.take() ?: return

        val ukupno = _questions.value.size
        val tacnih = correctAnswersCount

        val sesija = AssessmentSession(
            timestamp = System.currentTimeMillis(),
            materialId = p.materialId,
            runId = p.runId,
            participantId = p.participantId,
            condition = p.condition,
            initialWpm = p.initialWpm,
            finalWpm = p.finalWpm,
            wpmChangeCount = p.wpmChangeCount,
            activeReadingTimeMs = p.activeReadingTimeMs,
            totalElapsedTimeMs = p.totalElapsedTimeMs,
            wordsTotal = p.wordsTotal,
            wordsConsumed = p.wordsConsumed,
            effectiveWpm = AssessmentSession.computeWpm(
                p.wordsConsumed, p.activeReadingTimeMs
            ),
            sessionWpm = AssessmentSession.computeWpm(
                p.wordsConsumed, p.totalElapsedTimeMs
            ),
            alpha = p.alpha,
            backtrackCount = p.backtrackCount,
            correctAnswers = tacnih,
            totalQuestions = ukupno,
            algorithmVersion = "adaptive-v2"
        )

        viewModelScope.launch { assessmentDao.insertSession(sesija) }
    }
}
