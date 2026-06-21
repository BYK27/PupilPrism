package com.example.pupilprism.ui.library

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.pupilprism.data.db.UserStatsDao
import com.example.pupilprism.data.model.UserStats
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userStatsDao: UserStatsDao,
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stats by userStatsDao.getStatsFlow().collectAsState(initial = UserStats())

    val curatedColors: List<Color> = listOf(
        Color(0xFF6650a4), Color(0xFFc45e5e), Color(0xFFf79e3e),
        Color(0xFFffd261), Color(0xFF8fd993), Color(0xFF70a6db),
        Color(0xFF3e4959)
    )

    val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    var favoriteColors: List<Color> by remember {
        val savedFavorites = prefs.getString("favorites", "") ?: ""
        val loadedColors = if (savedFavorites.isNotBlank()) {
            savedFavorites.split(",")
                .mapNotNull { it.toIntOrNull() }
                .map { Color(it) }
        } else {
            emptyList<Color>()
        }
        mutableStateOf(loadedColors)
    }

    // Custom Color Sliders State
    var red by remember { mutableFloatStateOf(102f) }
    var green by remember { mutableFloatStateOf(80f) }
    var blue by remember { mutableFloatStateOf(164f) }
    val customColor = Color(red.toInt(), green.toInt(), blue.toInt())

    // --- NEW: Helper to sync sliders to a clicked color ---
    val syncSlidersToColor = { color: Color ->
        red = color.red * 255f
        green = color.green * 255f
        blue = color.blue * 255f
    }

    // Sync sliders on initial load if a theme is already set
    LaunchedEffect(Unit) {
        val initialStats = userStatsDao.getStats()
        if (initialStats != null) {
            syncSlidersToColor(Color(initialStats.themeColor))
        }
    }

    // --- NEW: Helper to save color instantly ---
    val saveThemeColor = { colorToSave: Color ->
        scope.launch {
            val current = stats ?: UserStats()
            userStatsDao.insertOrUpdate(current.copy(themeColor = colorToSave.toArgb()))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- Tinted Background Toggle ---
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tinted Background", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Applies a soft wash of your theme color to the background.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = stats?.isBackgroundEnabled ?: false,
                        onCheckedChange = { isEnabled ->
                            scope.launch {
                                val current = stats ?: UserStats()
                                userStatsDao.insertOrUpdate(current.copy(isBackgroundEnabled = isEnabled))
                            }
                        }
                    )
                }
            }

            Text("Theme Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // --- Curated Colors ---
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(curatedColors) { color ->
                    ColorCircle(color = color, isSelected = stats?.themeColor == color.toArgb()) {
                        syncSlidersToColor(color) // Sync the sliders
                        saveThemeColor(color)     // Apply the theme
                    }
                }
            }

            HorizontalDivider()

            // --- Custom Color Picker ---
            Text("Custom Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(customColor)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = red,
                        onValueChange = { red = it },
                        onValueChangeFinished = { saveThemeColor(customColor) },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Red,
                            activeTrackColor = Color.Red,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f) // Forces the track to be white
                        )
                    )
                    Slider(
                        value = green,
                        onValueChange = { green = it },
                        onValueChangeFinished = { saveThemeColor(customColor) },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Green,
                            activeTrackColor = Color.Green,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                        )
                    )
                    Slider(
                        value = blue,
                        onValueChange = { blue = it },
                        onValueChangeFinished = { saveThemeColor(customColor) },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Blue,
                            activeTrackColor = Color.Blue,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                        )
                    )
                }
            }

            Button(
                onClick = {
                    val newList: List<Color> = (favoriteColors + customColor).distinct().takeLast(5)
                    favoriteColors = newList
                    prefs.edit().putString("favorites", newList.joinToString(",") { it.toArgb().toString() }).apply()
                    saveThemeColor(customColor)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Save")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to Favorites")
            }

            // --- Favorites ---
            if (favoriteColors.isNotEmpty()) {
                Text("Favorites", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(favoriteColors.reversed()) { color ->
                        ColorCircle(color = color, isSelected = stats?.themeColor == color.toArgb()) {
                            syncSlidersToColor(color)
                            saveThemeColor(color)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 4.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}