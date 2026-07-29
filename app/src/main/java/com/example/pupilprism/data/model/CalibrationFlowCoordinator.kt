package com.example.pupilprism.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.pupilprism.ui.reader.CalibrationViewModel

@Composable
fun CalibrationFlowCoordinator(
    viewModel: CalibrationViewModel,
    navController: NavHostController
) {
    val state by viewModel.uiState.collectAsState()

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val correct = savedStateHandle?.getStateFlow<Int?>("quiz_correct", null)?.collectAsState()?.value
    val total = savedStateHandle?.getStateFlow<Int?>("quiz_total", null)?.collectAsState()?.value

    LaunchedEffect(correct, total) {
        if (correct != null && total != null) {
            viewModel.processStageResult(correct, total)
            savedStateHandle.set("quiz_correct", null)
            savedStateHandle.set("quiz_total", null)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading Assessment Materials...", color = MaterialTheme.colorScheme.onSurfaceVariant)

            } else if (state.isCalibrationComplete) {
                // Sleek Completion Screen
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Complete", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text("Assessment Complete", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Optimal Comprehension Speed", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${state.finalCalculatedWpm} WPM", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Return to Dashboard", fontSize = 16.sp)
                }

            } else {
                val currentMaterial = state.calibrationMaterials.getOrNull(state.currentStageIndex)
                val targetSpeed = viewModel.targetSpeeds.getOrNull(state.currentStageIndex) ?: 250

                if (currentMaterial != null) {
                    // Sleek Phase Introduction UI
                    Text(
                        text = "PHASE ${state.currentStageIndex + 1} OF ${viewModel.targetSpeeds.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                        letterSpacing = 1.5.sp
                    )

                    Text(
                        text = "Reading Assessment",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            // Makes the card blend perfectly into your tinted background
                            containerColor = Color.Transparent
                        ),
                        // Adds a sleek, modern outline matching your chosen theme color
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Speed", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Target Speed: $targetSpeed WPM",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "You will read a short text at a locked speed. Keep your eyes focused on the center. A short comprehension quiz will follow.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    navController.navigate("calibration_reader/${currentMaterial.id}/$targetSpeed")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Start Reading", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Rounded.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                } else {
                    // Fallback UI
                    Icon(Icons.Rounded.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Error: Calibration texts failed to load.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}