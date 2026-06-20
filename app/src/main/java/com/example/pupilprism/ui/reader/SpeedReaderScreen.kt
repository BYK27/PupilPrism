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

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReaderScreen(
    pdfUri: Uri,
    pdfName: String,
    type: String,
    pdfBookDao: PdfBookDao,
    userStatsDao: UserStatsDao,
    navController: NavHostController,
    rsvpViewModel: RSVPViewModel
) {
    val context = LocalContext.current

    // 1. Observe the state from the ViewModel
    val uiState by rsvpViewModel.uiState.collectAsState()

    var showFullText by remember { mutableStateOf(false) }
    var showPercentage by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf<UserStats?>(null) }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val themeColor = stats?.themeColor?.let { Color(it.toLong() and 0xFFFFFFFFL) } ?: MaterialTheme.colorScheme.primary
    val buttonColors = ButtonDefaults.buttonColors(containerColor = themeColor)
    val backgroundColor = if (stats?.isBackgroundEnabled == true) {
        themeColor.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.background
    }

    LaunchedEffect(Unit) {
        stats = withContext(Dispatchers.IO) { userStatsDao.getStats() }
    }

    LaunchedEffect(uiState.currentIndex, showFullText) {
        if (showFullText && uiState.words.isNotEmpty()) {
            listState.animateScrollToItem(uiState.currentIndex)
        }
    }

    LaunchedEffect(pdfUri) {
        val rawText = if (type == "web") {
            extractTextFromWeb(pdfUri.toString())
        } else {
            extractTextFromPdfCached(context, pdfUri)
        }

        val currentStats = withContext(Dispatchers.IO) { userStatsDao.getStats() }
        val baselineWpm = currentStats?.optimalWpm ?: 250

        rsvpViewModel.loadContent(
            text = rawText,
            id = pdfUri.toString(),
            startWpm = baselineWpm,
            isCalibration = false
        )
    }

    // 2. React to index changes to save PDF progress and calculate Streaks
    LaunchedEffect(uiState.currentIndex) {
        if (uiState.currentIndex > 0 && !uiState.isPaused) {
            if (type == "pdf") {
                withContext(Dispatchers.IO) {
                    pdfBookDao.insertOrUpdate(
                        PdfBook(uri = pdfUri.toString(), name = pdfName, lastWordIndex = uiState.currentIndex)
                    )
                }
            }

            withContext(Dispatchers.IO) {
                val today = java.time.LocalDate.now().toString()
                val s = stats ?: userStatsDao.getStats() ?: UserStats()

                val isNewDay = s.lastReadDate != today
                val newTodayCount = if (isNewDay) 1 else s.todayWords + 1
                val newTotal = s.totalWordsRead + 1

                val reached3000Today = newTodayCount >= 3000 && s.streakUpdatedDate != today
                val updatedStreak = if (reached3000Today) s.streak + 1 else s.streak

                val updatedStats = s.copy(
                    totalWordsRead = newTotal,
                    todayWords = newTodayCount,
                    lastReadDate = today,
                    streak = updatedStreak,
                    streakUpdatedDate = if (reached3000Today) today else s.streakUpdatedDate
                )

                userStatsDao.insertOrUpdate(updatedStats)
                stats = updatedStats
            }
        }
    }

    // 3. Time calculation based on ViewModel state
    val remainingWords = (uiState.words.size - uiState.currentIndex).coerceAtLeast(0)
    val timeLeftMinutes = if (uiState.wpm > 0) remainingWords.toDouble() / uiState.wpm else 0.0
    val totalMinutes = timeLeftMinutes.toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val timeFormatted = String.format("%dh %02dm", hours, minutes)

    Scaffold(
        containerColor = backgroundColor,
        topBar = { TopAppBar(title = { Text("Speed Reader") }, colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor
        )) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { showFullText = !showFullText }, colors = buttonColors) {
                        Text(if (showFullText) "Hide Text" else "Show Text")
                    }

                    Button(onClick = {
                        if (!uiState.isPaused) rsvpViewModel.togglePause()
                        navController.navigate("full_reader/$type/${Uri.encode(pdfUri.toString())}/$pdfName")
                    }, enabled = type == "web", colors = buttonColors) {
                        Text("Read as Book")
                    }

                    Button(onClick = {
                        if (!uiState.isPaused) rsvpViewModel.togglePause()
                        navController.navigate("eye_tracker/$type/${Uri.encode(pdfUri.toString())}/$pdfName")
                    }, enabled = type == "web", colors = buttonColors) {
                        Text("Eye Track")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.words.isEmpty()) {
                    Text("Loading PDF...", fontSize = 24.sp)
                } else {
                    Text(text = uiState.currentWord, fontSize = 48.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 4. Update UI to call ViewModel methods for state changes
                        Button(onClick = { rsvpViewModel.changeWpm(-50) }, colors = buttonColors) { Text("--") }
                        Button(onClick = { rsvpViewModel.changeWpm(-10) }, colors = buttonColors) { Text("-") }

                        Text("WPM: ${uiState.wpm}", fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterVertically))

                        Button(onClick = { rsvpViewModel.changeWpm(10) }, colors = buttonColors) { Text("+") }
                        Button(onClick = { rsvpViewModel.changeWpm(50) }, colors = buttonColors) { Text("++") }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Switch(
                            checked = uiState.isAdaptiveSpeedEnabled,
                            onCheckedChange = { rsvpViewModel.toggleAdaptiveSpeed() }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adaptive Speed", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { rsvpViewModel.togglePause() }, colors = buttonColors) {
                        Text(if (uiState.isPaused) "Play" else "Pause")
                    }

                    if (showFullText) {
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.foundation.lazy.LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp)
                        ) {
                            itemsIndexed(uiState.words) { index, word ->
                                Text(
                                    text = word,
                                    fontSize = 16.sp,
                                    color = if (index == uiState.currentIndex)
                                        androidx.compose.ui.graphics.Color.Red
                                    else
                                        androidx.compose.ui.graphics.Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(2.dp)
                                    // Wait until you implement a jumpTo method in ViewModel
                                    // .clickable { rsvpViewModel.jumpToIndex(index) }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showPercentage && uiState.words.isNotEmpty())
                        "${(uiState.currentIndex * 100 / uiState.words.size)}%"
                    else
                        "${uiState.currentIndex + 1}/${uiState.words.size}",
                    modifier = Modifier.clickable { showPercentage = !showPercentage },
                    fontSize = 14.sp
                )

                Text(
                    text = timeFormatted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}

// --- Helper function to extract text from a PDF ---
fun extractTextFromPdf(context: Context, pdfUri: Uri): String {
    var text = ""
    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(pdfUri)
        inputStream?.use { stream ->
            val document = PDDocument.load(stream)
            val stripper = PDFTextStripper()
            text = stripper.getText(document)
            document.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return text
}

suspend fun extractTextFromPdfCached(context: Context, pdfUri: Uri): String = withContext(Dispatchers.IO) {
    try {
        val cacheDir = context.getExternalFilesDir("pdf_cache")
        if (cacheDir?.exists() == false) cacheDir.mkdirs()

        val cacheFile = File(cacheDir, "${pdfUri.hashCode()}.txt")

        if (cacheFile.exists()) {
            return@withContext cacheFile.readText()
        }

        val inputStream: InputStream? = context.contentResolver.openInputStream(pdfUri)
        val text = buildString {
            inputStream?.use { stream ->
                PDDocument.load(stream).use { document ->
                    val stripper = PDFTextStripper()
                    append(stripper.getText(document))
                }
            }
        }

        cacheFile.writeText(text)

        return@withContext text
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

suspend fun extractTextFromWeb(url: String): String = withContext(Dispatchers.IO) {
    try {
        // Connect and fetch the HTML document
        val doc = Jsoup.connect(url).get()

        // 1. Clean the document: Remove common "fluff" elements
        doc.select("nav, footer, header, aside, script, style, noscript, .sidebar, .menu, #comments").remove()

        // 2. Locate the main content area
        val mainContainer = doc.selectFirst("article")
            ?: doc.selectFirst("main")
            ?: doc.selectFirst("[role=main]")
            ?: doc.body() // Fallback to the cleaned body if no semantic tags exist

        // 3. Extract readable text block by block (Headers and Paragraphs)
        val readableElements = mainContainer.select("h1, h2, h3, h4, h5, h6, p")

        // 4. Return the compiled text
        if (readableElements.isNotEmpty()) {
            return@withContext readableElements.joinToString(" ") { it.text() }
        } else {
            // Absolute fallback in case the page doesn't use standard <p> tags
            return@withContext mainContainer.text()
        }

    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext "Error: Could not load text from this URL."
    }
}