package com.example.pupilprism.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
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
import com.example.pupilprism.ui.reader.SelfPacedViewModel
import com.example.pupilprism.ui.report.ReportScreen

@Composable
fun SelfPacedFlowCoordinator(
    viewModel: SelfPacedViewModel,
    navController: NavHostController
) {
    val state by viewModel.uiState.collectAsState()

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val total = savedStateHandle?.getStateFlow<Int?>("quiz_total", null)?.collectAsState()?.value

    val report by viewModel.report.collectAsState()

    LaunchedEffect(total) {
        if (total != null) {
            viewModel.onTextFinished()
            savedStateHandle?.set<Int?>("quiz_correct", null)
            savedStateHandle?.set<Int?>("quiz_total", null)
        }
    }

    if (state.isComplete) {
        val r = report
        if (r != null) {
            ReportScreen(
                report = r,
                onZavrsi = { navController.popBackStack("library", inclusive = false) }
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading self-paced texts...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val currentMaterial = state.materials.getOrNull(state.currentStageIndex)

                if (currentMaterial != null) {
                    Text(
                        text = "TEXT ${state.currentStageIndex + 1} OF ${state.materials.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Self-Paced Reading",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
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
                                text = "Starting Speed: ${state.startWpm} WPM",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "You control the speed this time, and you can go back to re-read a word. A comprehension quiz follows.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    navController.navigate("self_paced_reader/${currentMaterial.id}/${state.startWpm}")
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Start Reading", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Rounded.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                } else {
                    Icon(Icons.Rounded.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Error: Self-paced texts failed to load.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}