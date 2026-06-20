package com.example.pupilprism.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pupilprism.data.db.AssessmentDao
import com.example.pupilprism.data.model.ComprehensionQuestion
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
            // Load questions from DB for this material
            _questions.value = assessmentDao.getQuestionsForMaterial(materialId)
            _currentIndex.value = 0
            correctAnswersCount = 0
        }
    }

    // Returns TRUE if the quiz is finished, FALSE if there are more questions
    fun submitAnswer(selectedIndex: Int): Boolean {
        val currentQuestion = _questions.value[_currentIndex.value]

        if (selectedIndex == currentQuestion.correctAnswerIndex) {
            correctAnswersCount++
        }

        val isFinished = _currentIndex.value >= _questions.value.size - 1

        if (isFinished) {
            // Save results to DB / calculate score here
            saveSessionResults()
            return true
        } else {
            _currentIndex.value += 1
            return false
        }
    }

    private fun saveSessionResults() {
        // Implement logic to save the SessionData / Results to AssessmentDao
    }
}