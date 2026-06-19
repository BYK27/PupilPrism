package com.example.pupilprism.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pupilprism.data.db.AssessmentDao
import com.example.pupilprism.data.model.AssessmentSession
import com.example.pupilprism.data.model.ComprehensionQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuizViewModel(private val dao: AssessmentDao) : ViewModel() {
    private val _questions = MutableStateFlow<List<ComprehensionQuestion>>(emptyList())
    val questions = _questions.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    private var correctCount = 0

    fun loadQuestions(materialId: String) {
        viewModelScope.launch {
            _questions.value = dao.getQuestionsForMaterial(materialId)
        }
    }

    fun submitAnswer(selectedIndex: Int, sessionData: AssessmentSession) {
        val currentQ = _questions.value[_currentIndex.value]
        if (selectedIndex == currentQ.correctAnswerIndex) {
            correctCount++
        }

        if (_currentIndex.value < _questions.value.size - 1) {
            _currentIndex.value++
        } else {
            finishQuiz(sessionData)
        }
    }

    private fun finishQuiz(sessionData: AssessmentSession) {
        val finalSession = sessionData.copy(
            correctAnswers = correctCount,
            totalQuestions = _questions.value.size
        )
        viewModelScope.launch {
            dao.insertSession(finalSession)
        }
    }
}