package com.example.pupilprism.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.pupilprism.ui.reader.CalibrationViewModel

@Composable
fun CalibrationFlowCoordinator(
    viewModel: CalibrationViewModel,
    navController: NavHostController
) {
    val state by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else if (state.isCalibrationComplete) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Complete", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Assessment Complete!", style = MaterialTheme.typography.headlineMedium)
                Text("Your optimal comprehension speed is: ${state.finalCalculatedWpm} WPM", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Return to Library")
                }
            } else {
                val currentMaterial = state.calibrationMaterials.getOrNull(state.currentStageIndex)
                val targetSpeed = viewModel.targetSpeeds.getOrNull(state.currentStageIndex) ?: 250

                if (currentMaterial != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        // ... Your existing Card content ...
                    }
                } else {
                    // Fallback UI to prevent the blank screen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Error: Calibration texts failed to load.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Please try reinstalling the app or checking the database configuration.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

        }
    }
}