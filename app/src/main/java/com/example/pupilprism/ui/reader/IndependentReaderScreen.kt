package com.example.pupilprism.ui.reader

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.pupilprism.data.db.PdfBookDao
import com.example.pupilprism.data.db.UserStatsDao
import com.example.pupilprism.data.model.PdfBook
import com.example.pupilprism.data.model.RSVPViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndependentReaderScreen(
    pdfUri: Uri,
    pdfName: String,
    type: String,
    pdfBookDao: PdfBookDao,
    userStatsDao: UserStatsDao,
    navController: NavHostController,
    rsvpViewModel: RSVPViewModel
) {
    val context = LocalContext.current
    val uiState by rsvpViewModel.uiState.collectAsState()
    var isLoaded by remember { mutableStateOf(false) }

    // 1. Initial Load: Parse the document and fetch reading history
    LaunchedEffect(pdfUri) {
        val stats = withContext(Dispatchers.IO) { userStatsDao.getStats() }
        val startWpm = stats?.optimalWpm ?: 250

        val extractedText = withContext(Dispatchers.IO) {
            if (type == "web") extractTextFromWeb(pdfUri.toString())
            else extractTextFromPdfCached(context, pdfUri)
        }

        val savedBook = withContext(Dispatchers.IO) {
            pdfBookDao.getAll().find { it.uri == pdfUri.toString() }
        }
        val startIndex = savedBook?.lastWordIndex ?: 0

        rsvpViewModel.loadContent(extractedText, pdfUri.toString(), startWpm, false, startIndex)
        isLoaded = true
    }

    // 2. Continuous Tracking: Save progress every 10 words to prevent DB lag
    LaunchedEffect(uiState.currentIndex) {
        if (isLoaded && uiState.currentIndex > 0 && uiState.currentIndex % 10 == 0) {
            withContext(Dispatchers.IO) {
                if (type == "pdf") {
                    pdfBookDao.insertOrUpdate(PdfBook(uri = pdfUri.toString(), name = pdfName, lastWordIndex = uiState.currentIndex))
                }

                // Track user reading streaks
                val today = LocalDate.now().toString()
                val s = userStatsDao.getStats()
                if (s != null) {
                    val isNewDay = s.lastReadDate != today
                    val newTodayCount = if (isNewDay) 10 else s.todayWords + 10
                    val newTotal = s.totalWordsRead + 10
                    val reached3000Today = newTodayCount >= 3000 && s.streakUpdatedDate != today
                    val updatedStreak = if (reached3000Today) s.streak + 1 else s.streak

                    userStatsDao.insertOrUpdate(s.copy(
                        totalWordsRead = newTotal,
                        todayWords = newTodayCount,
                        lastReadDate = today,
                        streak = updatedStreak,
                        streakUpdatedDate = if (reached3000Today) today else s.streakUpdatedDate
                    ))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = uiState.isPaused, enter = fadeIn(), exit = fadeOut()) {
                TopAppBar(
                    title = { Text(pdfName, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Text("${(uiState.progressPercentage * 100).toInt()}%", modifier = Modifier.padding(end = 16.dp))
                    }
                )
            }
        }
    ) { padding ->
        if (!isLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = uiState.progressPercentage,
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Focus Reading Canvas
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().clickable { rsvpViewModel.togglePause() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.currentWord,
                    fontSize = 56.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            AnimatedVisibility(visible = uiState.isPaused, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                            Switch(
                                checked = uiState.isAdaptiveSpeedEnabled,
                                onCheckedChange = { rsvpViewModel.toggleAdaptiveSpeed() }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Adaptive Punctuation Delay", style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                        ) {
                            IconButton(onClick = { rsvpViewModel.changeWpm(-10) }) { Text("-", fontSize = 24.sp) }
                            Text("${uiState.wpm} WPM", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { rsvpViewModel.changeWpm(10) }) { Text("+", fontSize = 24.sp) }
                        }

                        FloatingActionButton(
                            onClick = { rsvpViewModel.togglePause() },
                            modifier = Modifier.size(72.dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play/Pause", modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }
        }
    }
}