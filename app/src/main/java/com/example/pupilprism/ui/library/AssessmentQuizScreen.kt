package com.example.pupilprism.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.pupilprism.ui.reader.QuizViewModel

@Composable
fun AssessmentQuizScreen(
    viewModel: QuizViewModel,
    materialId: String,
    navController: NavHostController
) {
    val questions by viewModel.questions.collectAsState()
    val index by viewModel.currentIndex.collectAsState()

    LaunchedEffect(materialId) {
        viewModel.loadQuestions(materialId)
    }

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentQ = questions[index]

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            LinearProgressIndicator(
                progress = (index + 1) / questions.size.toFloat(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )

            Text("Question ${index + 1} of ${questions.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(12.dp))

            Text(currentQ.questionText, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            val options = listOf(currentQ.optionA, currentQ.optionB, currentQ.optionC, currentQ.optionD)

            options.forEachIndexed { i, text ->
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .heightIn(min = 56.dp),
                    shape = MaterialTheme.shapes.medium,
                    onClick = {
                        val isFinished = viewModel.submitAnswer(i)

                        if (isFinished) {
                            navController.previousBackStackEntry?.savedStateHandle?.set("quiz_correct", viewModel.correctAnswersCount)
                            navController.previousBackStackEntry?.savedStateHandle?.set("quiz_total", questions.size)
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                }
            }
        }
    }
}