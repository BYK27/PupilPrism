package com.example.pupilprism.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.pupilprism.data.db.PdfBookDao
import com.example.pupilprism.data.db.UserStatsDao
import com.example.pupilprism.data.model.PdfBook
import com.example.pupilprism.data.model.RSVPViewModel
import com.example.pupilprism.data.model.UserStats
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*


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

    // Automatically navigate to Quiz if calibration mode text is finished
    LaunchedEffect(uiState.isFinished) {
        if (isCalibrationMode && uiState.isFinished && materialId != null) {
            navController.navigate("quiz/$materialId") {
                popUpTo("calibration_reader") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCalibrationMode) "Assessment Progress" else "Reader") },
                actions = {
                    Text("${(uiState.progressPercentage * 100).toInt()}%", modifier = Modifier.padding(end = 16.dp))
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // The Reading Canvas (Focus Area)
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.currentWord,
                    fontSize = 56.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }

            // Bottom Controls Card
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

                    // Hide speed controls if calibrating
                    AnimatedVisibility(visible = !isCalibrationMode) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            IconButton(onClick = { rsvpViewModel.changeWpm(-10) }) { Text("-") }
                            Text("${uiState.wpm} WPM", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { rsvpViewModel.changeWpm(10) }) { Text("+") }
                        }
                    }

                    if (isCalibrationMode) {
                        Text("Locked at $calibrationWpm WPM", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    FloatingActionButton(
                        onClick = { rsvpViewModel.togglePause() },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Lock,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}