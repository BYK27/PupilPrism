package com.example.pupilprism.ui.reader

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
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
import com.example.pupilprism.data.model.ProgressDisplayMode
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
    var selectedTab by remember { mutableIntStateOf(0) }

    // NEW: SharedPreferences for global settings
    val prefs = context.getSharedPreferences("reader_prefs", android.content.Context.MODE_PRIVATE)

    // NEW: Load saved settings on first launch
    LaunchedEffect(Unit) {
        val savedAdaptiveSpeed = prefs.getBoolean("adaptive_speed", false)
        val savedDisplayMode = prefs.getString("display_mode", ProgressDisplayMode.PERCENTAGE.name) ?: ProgressDisplayMode.PERCENTAGE.name
        val savedMultiWord = prefs.getBoolean("multi_word", false)

        // Sync ViewModel with saved preferences
        if (uiState.isAdaptiveSpeedEnabled != savedAdaptiveSpeed) rsvpViewModel.toggleAdaptiveSpeed()
        if (uiState.displayMode.name != savedDisplayMode) rsvpViewModel.toggleProgressDisplayMode()
        if (uiState.isMultiWordMode != savedMultiWord) rsvpViewModel.toggleMultiWordMode()
    }

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

    // 2. Continuous Tracking
    LaunchedEffect(uiState.currentIndex) {
        if (isLoaded && uiState.currentIndex > 0 && uiState.currentIndex % 10 == 0) {
            withContext(Dispatchers.IO) {
                if (type == "pdf") {
                    pdfBookDao.insertOrUpdate(PdfBook(uri = pdfUri.toString(), name = pdfName, lastWordIndex = uiState.currentIndex))
                }

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
                        // NEW: Toggle between Percentage and Total Fraction mode
                        val progressText = if (uiState.displayMode == ProgressDisplayMode.PERCENTAGE) {
                            "${(uiState.progressPercentage * 100).toInt()}%"
                        } else {
                            "${uiState.currentIndex} / ${uiState.words.size}"
                        }
                        Text(progressText, modifier = Modifier.padding(end = 16.dp))
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // Reattach simple tap-to-pause only if single word mode is active
                    .then(if (!uiState.isMultiWordMode) Modifier.clickable { rsvpViewModel.togglePause() } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isMultiWordMode) {
                    val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, uiState.currentIndex - 2))

                    // Automatically scroll context alongside the reading pace
                    LaunchedEffect(uiState.currentIndex) {
                        if (!uiState.isPaused) {
                            listState.scrollToItem(maxOf(0, uiState.currentIndex - 2))
                        }
                    }

                    // CHANGED: LazyRow is now LazyColumn
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        // CHANGED: Padding is now vertical to allow space at the top and bottom
                        contentPadding = PaddingValues(vertical = 120.dp),
                        // CHANGED: Use verticalArrangement and horizontalAlignment
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(uiState.words.size) { index ->
                            val word = uiState.words[index]
                            val isCurrent = index == uiState.currentIndex
                            val alpha = if (isCurrent) 1f else 0.3f

                            Text(
                                text = word,
                                fontSize = if (isCurrent) 56.sp else 36.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.clickable {
                                    // CHANGED: If playing, any tap pauses it. Jumps only happen when paused.
                                    if (!uiState.isPaused) {
                                        rsvpViewModel.togglePause()
                                    } else if (isCurrent) {
                                        rsvpViewModel.togglePause()
                                    } else {
                                        rsvpViewModel.jumpToIndex(index)
                                    }
                                }
                            )
                        }
                    }

                } else {
                    Text(
                        text = uiState.currentWord,
                        fontSize = 56.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            AnimatedVisibility(visible = uiState.isPaused, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                        // NEW: Settings and Controls Tabs
                        TabRow(selectedTabIndex = selectedTab, modifier = Modifier.padding(bottom = 16.dp)) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                                Text("Controls", modifier = Modifier.padding(16.dp))
                            }
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                                Text("Settings", modifier = Modifier.padding(16.dp))
                            }
                        }

                        if (selectedTab == 0) {
                            // Controls View
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                            ) {
                                IconButton(onClick = { rsvpViewModel.changeWpm(-50) }) { Text("--", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                                IconButton(onClick = { rsvpViewModel.changeWpm(-10) }) { Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                                Text("${uiState.wpm} WPM", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { rsvpViewModel.changeWpm(10) }) { Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                                IconButton(onClick = { rsvpViewModel.changeWpm(50) }) { Text("++", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                            }

                            FloatingActionButton(
                                onClick = { rsvpViewModel.togglePause() },
                                modifier = Modifier.size(72.dp),
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play/Pause", modifier = Modifier.size(36.dp))
                            }
                        } else {
                            // Settings View
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Text("Adaptive Punctuation Delay", style = MaterialTheme.typography.bodyLarge)
                                    Switch(
                                        checked = uiState.isAdaptiveSpeedEnabled,
                                        onCheckedChange = {
                                            rsvpViewModel.toggleAdaptiveSpeed()
                                            prefs.edit().putBoolean("adaptive_speed", !uiState.isAdaptiveSpeedEnabled).apply()
                                        }
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Text("Show Progress as Fraction", style = MaterialTheme.typography.bodyLarge)
                                    Switch(
                                        checked = uiState.displayMode == ProgressDisplayMode.FRACTION,
                                        onCheckedChange = {
                                            rsvpViewModel.toggleProgressDisplayMode()
                                            val newMode = if (uiState.displayMode == ProgressDisplayMode.PERCENTAGE) ProgressDisplayMode.FRACTION else ProgressDisplayMode.PERCENTAGE
                                            prefs.edit().putString("display_mode", newMode.name).apply()
                                        }
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Text("Multi-word Context Mode", style = MaterialTheme.typography.bodyLarge)
                                    Switch(
                                        checked = uiState.isMultiWordMode,
                                        onCheckedChange = {
                                            rsvpViewModel.toggleMultiWordMode()
                                            prefs.edit().putBoolean("multi_word", !uiState.isMultiWordMode).apply()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}