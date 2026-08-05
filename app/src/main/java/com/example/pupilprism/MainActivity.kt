package com.example.pupilprism

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pupilprism.PupilPrismApp
import com.example.pupilprism.data.db.SeedManager
import com.example.pupilprism.data.model.RSVPViewModel
import com.example.pupilprism.di.LocalAppContainer
import com.example.pupilprism.di.VMFactory
import com.example.pupilprism.ui.library.AssessmentQuizScreen
import com.example.pupilprism.ui.library.CalibrationFlowCoordinator
import com.example.pupilprism.ui.library.LibraryScreen
import com.example.pupilprism.ui.library.SelfPacedFlowCoordinator
import com.example.pupilprism.ui.library.SettingsScreen
import com.example.pupilprism.ui.reader.CalibrationViewModel
import com.example.pupilprism.ui.reader.IndependentReaderScreen
import com.example.pupilprism.ui.reader.QuizViewModel
import com.example.pupilprism.ui.reader.SelfPacedViewModel
import com.example.pupilprism.ui.reader.SpeedReaderScreen
import com.example.pupilprism.ui.theme.SpeedReaderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as PupilPrismApp).container

        lifecycleScope.launch {
            SeedManager.syncIfNeeded(applicationContext, container.database)
        }

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                val userStats by container.database.userStatsDao()
                    .getStatsFlow()
                    .collectAsState(initial = null)

                SpeedReaderTheme(userStats = userStats) {
                    AppNavHost()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val container = LocalAppContainer.current
    val factory = VMFactory.create(container)

    val pdfBookDao = container.database.pdfBookDao()
    val userStatsDao = container.database.userStatsDao()

    NavHost(navController = navController, startDestination = "library") {

        composable("library") {
            LibraryScreen(
                pdfBookDao = pdfBookDao,
                userStatsDao = userStatsDao,
                navController = navController,
                onPdfSelected = { uri, name ->
                    navController.navigate("reader/pdf/${Uri.encode(uri.toString())}/$name")
                },
                onUrlSelected = { url ->
                    navController.navigate("reader/web/${Uri.encode(url)}/Web Article")
                }
            )
        }

        composable("settings") {
            SettingsScreen(userStatsDao = userStatsDao, navController = navController)
        }

        // Самостално читање корисниковог документа.
        // У затеченој верзији ова путања била је регистрована ДВАПУТ, једном за
        // SpeedReaderScreen и једном за IndependentReaderScreen. Задржана је
        // друга, јер нуди пун скуп контрола.
        composable("reader/{type}/{uri}/{name}") { entry ->
            val type = entry.arguments?.getString("type") ?: "db"
            val uri = Uri.parse(Uri.decode(entry.arguments?.getString("uri") ?: ""))
            val name = entry.arguments?.getString("name") ?: "Unknown"

            val rsvpViewModel: RSVPViewModel = viewModel(factory = factory)

            IndependentReaderScreen(
                pdfUri = uri,
                pdfName = name,
                type = type,
                pdfBookDao = pdfBookDao,
                userStatsDao = userStatsDao,
                navController = navController,
                rsvpViewModel = rsvpViewModel
            )
        }

        composable("calibration_flow") {
            val calibrationViewModel: CalibrationViewModel = viewModel(factory = factory)
            CalibrationFlowCoordinator(calibrationViewModel, navController)
        }

        // Читање при закљученој брзини (услов А из поглавља 5 рада).
        composable("calibration_reader/{materialId}/{wpm}") { entry ->
            val materialId = entry.arguments?.getString("materialId") ?: ""
            val wpm = entry.arguments?.getString("wpm")?.toIntOrNull() ?: 250

            val rsvpViewModel: RSVPViewModel = viewModel(factory = factory)

            SpeedReaderScreen(
                pdfUri = Uri.EMPTY,
                pdfName = "Assessment",
                type = "db",
                isCalibrationMode = true,
                calibrationWpm = wpm,
                pdfBookDao = pdfBookDao,
                userStatsDao = userStatsDao,
                navController = navController,
                rsvpViewModel = rsvpViewModel,
                materialId = materialId
            )
        }

        composable("quiz/{materialId}") { entry ->
            val materialId = entry.arguments?.getString("materialId") ?: ""
            val quizViewModel: QuizViewModel = viewModel(factory = factory)
            AssessmentQuizScreen(quizViewModel, materialId, navController)
        }

        composable("self_paced_flow") {
            val selfPacedViewModel: SelfPacedViewModel = viewModel(factory = factory)
            SelfPacedFlowCoordinator(selfPacedViewModel, navController)
        }

        composable("self_paced_reader/{materialId}/{wpm}") { entry ->
            val materialId = entry.arguments?.getString("materialId") ?: ""
            val wpm = entry.arguments?.getString("wpm")?.toIntOrNull() ?: 250

            val rsvpViewModel: RSVPViewModel = viewModel(factory = factory)

            SpeedReaderScreen(
                pdfUri = Uri.EMPTY,
                pdfName = "Assessment",
                type = "db",
                isCalibrationMode = true,
                lockControls = false,
                flowAnchorRoute = "self_paced_flow",
                calibrationWpm = wpm,
                pdfBookDao = pdfBookDao,
                userStatsDao = userStatsDao,
                navController = navController,
                rsvpViewModel = rsvpViewModel,
                materialId = materialId
            )
        }
    }
}
