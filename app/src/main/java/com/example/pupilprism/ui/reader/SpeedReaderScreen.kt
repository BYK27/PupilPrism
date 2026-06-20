package com.example.pupilprism.ui.reader

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.pupilprism.data.db.PdfBookDao
import com.example.pupilprism.data.db.UserStatsDao
import com.example.pupilprism.data.model.RSVPViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReaderScreen(
    pdfUri: Uri,
    pdfName: String,
    type: String,
    isCalibrationMode: Boolean,
    calibrationWpm: Int,
    pdfBookDao: PdfBookDao,
    userStatsDao: UserStatsDao,
    navController: NavHostController,
    rsvpViewModel: RSVPViewModel,
    materialId: String? = null
) {
    val uiState by rsvpViewModel.uiState.collectAsState()

    // 1. Fetch the text from the DB when the screen loads
    LaunchedEffect(materialId) {
        if (isCalibrationMode && materialId != null) {
            rsvpViewModel.loadMaterialFromDb(materialId, calibrationWpm, true)
        }
    }

    // 2. Automatically navigate to Quiz if calibration mode text is finished
    LaunchedEffect(uiState.isFinished) {
        if (isCalibrationMode && uiState.isFinished && materialId != null) {
            navController.navigate("quiz/$materialId") {
                // Safely clear the reader off the stack by anchoring to the coordinator
                popUpTo("calibration_flow") { inclusive = false }
            }
        }
    }

    Scaffold(
        topBar = {
            // Hide the TopAppBar when reading is active for a distraction-free UI
            AnimatedVisibility(visible = uiState.isPaused, enter = fadeIn(), exit = fadeOut()) {
                TopAppBar(
                    title = { Text(if (isCalibrationMode) "Assessment" else "Reader") },
                    actions = {
                        Text("${(uiState.progressPercentage * 100).toInt()}%", modifier = Modifier.padding(end = 16.dp))
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Minimalist Progress Bar replacing the percentage text
            LinearProgressIndicator(
                progress = uiState.progressPercentage,
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // The Reading Canvas (Focus Area)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.currentWord,
                    fontSize = 56.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Bottom Controls Card - Fades out when playing
            AnimatedVisibility(visible = uiState.isPaused, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        if (isCalibrationMode) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.padding(bottom = 24.dp)
                            ) {
                                Text(
                                    "Locked at $calibrationWpm WPM",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp)
                            ) {
                                IconButton(onClick = { rsvpViewModel.changeWpm(-10) }) { Text("-", fontSize = 24.sp) }
                                Text("${uiState.wpm} WPM", style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                IconButton(onClick = { rsvpViewModel.changeWpm(10) }) { Text("+", fontSize = 24.sp) }
                            }
                        }

                        FloatingActionButton(
                            onClick = { rsvpViewModel.togglePause() },
                            modifier = Modifier.size(72.dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = if (uiState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Lock,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}