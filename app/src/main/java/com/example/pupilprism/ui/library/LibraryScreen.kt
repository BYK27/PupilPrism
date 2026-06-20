package com.example.pupilprism.ui.library

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import java.io.File

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.pupilprism.data.db.PdfBookDao
import com.example.pupilprism.data.db.UserStatsDao
import com.example.pupilprism.data.model.UserStats
import kotlinx.coroutines.launch

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    pdfBookDao: PdfBookDao,
    userStatsDao: UserStatsDao,
    navController: NavHostController,
    onPdfSelected: (Uri, String) -> Unit,
    onUrlSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdfList by remember { mutableStateOf(listOf<com.example.pupilprism.data.model.PdfBook>()) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var pdfToRename by remember { mutableStateOf<com.example.pupilprism.data.model.PdfBook?>(null) }
    var newName by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var stats by remember { mutableStateOf<UserStats?>(null) }

    var showUrlDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }

    LaunchedEffect(userStatsDao) {
        val today = java.time.LocalDate.now()
        val s = userStatsDao.getStats() ?: UserStats()

        val lastRead = if (s.lastReadDate.isNotBlank()) java.time.LocalDate.parse(s.lastReadDate) else null

        val updatedStats = if (lastRead == null || lastRead.isBefore(today)) {
            // Determine yesterday's words
            val yesterdayWords = if (lastRead == today.minusDays(1)) s.todayWords else 0

            // Reset streak if yesterday < 3000 or last reading >1 day ago
            val newStreak = if (yesterdayWords >= 3000) s.streak else 0

            s.copy(
                streak = newStreak,
                todayWords = 0,
                yesterdayWords = yesterdayWords,
                lastReadDate = today.toString(),
                streakUpdatedDate = "" // allow streak increment today
            )
        } else {
            s
        }

        userStatsDao.insertOrUpdate(updatedStats)
        stats = updatedStats

        android.util.Log.d("SpeedReaderStats", "LibraryScreen new day check: $updatedStats")
    }

    // PDF picker
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                val contentResolver = context.contentResolver

                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }

                val realFileName = getFileNameFromUri(context, uri)

                scope.launch {
                    val destFile = File(context.getExternalFilesDir(null), realFileName)

                    contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    val savedBook = com.example.pupilprism.data.model.PdfBook(
                        uri = destFile.toUri().toString(),
                        name = realFileName
                    )
                    pdfBookDao.insertOrUpdate(savedBook)
                    pdfList = pdfBookDao.getAll()
                }
            }
        }
    )

    // Load PDFs from DB
    LaunchedEffect(Unit) {
        pdfList = pdfBookDao.getAll()
    }

    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("Dashboard") }) },
        floatingActionButton = { /* ... keep existing FAB ... */ }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Assessment Call to Action
            item {
                ElevatedCard(
                    onClick = { navController.navigate("calibration_flow") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reading Assessment", style = MaterialTheme.typography.titleLarge)
                            Text("Determine your optimal WPM based on PISA comprehension tests.", style = MaterialTheme.typography.bodyMedium)
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Start", modifier = Modifier.size(32.dp))
                    }
                }
            }

            // User Stats Row
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Streak", "${stats?.streak ?: 0} days", Modifier.weight(1f))
                    StatCard("Optimal", "${stats?.optimalWpm ?: 250} WPM", Modifier.weight(1f))
                    StatCard("Read Today", "${stats?.todayWords ?: 0} words", Modifier.weight(1f))
                }
            }

            item { Text("Your Library", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }

            // Document List
            items(pdfList) { pdf ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { onPdfSelected(Uri.parse(pdf.uri), pdf.name) }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = "PDF", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = pdf.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// --- Helper to load a default PDF ---
fun getDefaultPdfUri(context: android.content.Context): Uri {
    val file = File(context.cacheDir, "default.pdf")
    if (!file.exists()) {
        context.assets.open("default.pdf").use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    return file.toUri()
}

// --- Helper to get name from pdf ---
@SuppressLint("Range")
fun getFileNameFromUri(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        } finally {
            cursor?.close()
        }
    }
    // Fallback if it's a standard file URI
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "Unknown.pdf"
}