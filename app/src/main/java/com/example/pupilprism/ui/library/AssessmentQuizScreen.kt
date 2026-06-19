package com.example.pupilprism.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pupilprism.data.model.AssessmentSession
import com.example.pupilprism.ui.reader.QuizViewModel

@Composable
fun AssessmentQuizScreen(viewModel: QuizViewModel, sessionData: AssessmentSession) {
    val questions by viewModel.questions.collectAsState()
    val index by viewModel.currentIndex.collectAsState()

    if (questions.isEmpty()) {
        Text("Loading quiz...")
        return
    }

    val currentQ = questions[index]

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Question ${index + 1} of ${questions.size}", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(currentQ.questionText, style = MaterialTheme.typography.headlineSmall)

        val options = listOf(currentQ.optionA, currentQ.optionB, currentQ.optionC, currentQ.optionD)

        options.forEachIndexed { i, text ->
            Button(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                onClick = { viewModel.submitAnswer(i, sessionData) }
            ) {
                Text(text)
            }
        }
    }
}