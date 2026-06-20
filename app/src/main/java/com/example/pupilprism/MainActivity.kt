package com.example.speedreader

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pupilprism.data.db.AppDatabase
import com.example.pupilprism.data.db.AssessmentDao
import com.example.pupilprism.data.db.PdfBookDao
import com.example.pupilprism.data.db.UserStatsDao
import com.example.pupilprism.data.model.RSVPViewModel
import com.example.pupilprism.ui.library.LibraryScreen
import com.example.pupilprism.ui.reader.EyeTrackingReaderScreen
import com.example.pupilprism.ui.reader.FullPdfScreen
import com.example.pupilprism.ui.reader.SpeedReaderScreen
import com.example.pupilprism.ui.theme.SpeedReaderTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        PDFBoxResourceLoader.init(applicationContext)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "speedreader-db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).build()
        setContent {
            SpeedReaderTheme {
                val navController = rememberNavController()
                AppNavigation(navController, db.pdfBookDao(), db.userStatsDao(), db.assessmentDao())
            }
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    pdfBookDao: PdfBookDao,
    userStatsDao: UserStatsDao,
    assessmentDao: AssessmentDao
) {
    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(pdfBookDao = pdfBookDao, userStatsDao = userStatsDao, onPdfSelected = { uri, name ->
                navController.navigate("reader/pdf/${Uri.encode(uri.toString())}/$name")
            }, onUrlSelected = { url ->
                navController.navigate("reader/web/${Uri.encode(url)}/Web Article")
            })
        }

        composable("reader/{type}/{uri}/{name}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "pdf"
            val uri = Uri.parse(backStackEntry.arguments?.getString("uri"))
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"

            // 2. Create the RSVPViewModel using a factory
            val rsvpViewModel: RSVPViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return RSVPViewModel(assessmentDao) as T
                    }
                }
            )

            SpeedReaderScreen(uri, name, type, pdfBookDao, userStatsDao, navController, rsvpViewModel)
        }

        // MODIFIED: Added {type} to the route and parameters [cite: 5]
        composable("full_reader/{type}/{uri}/{name}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "pdf"
            val uri = Uri.parse(backStackEntry.arguments?.getString("uri"))
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"

            FullPdfScreen(uri, name, type, pdfBookDao, navController)
        }

        // MODIFIED: Added {type} to the route and parameters [cite: 5, 6]
        composable("eye_tracker/{type}/{uri}/{name}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "pdf"
            val uri = Uri.parse(backStackEntry.arguments?.getString("uri"))
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"

            EyeTrackingReaderScreen(uri, name, type, navController)
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_stats (
                id INTEGER NOT NULL PRIMARY KEY,
                totalWordsRead INTEGER NOT NULL,
                todayWords INTEGER NOT NULL,
                lastReadDate TEXT NOT NULL,
                streak INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE user_stats ADD COLUMN streakUpdatedDate TEXT NOT NULL DEFAULT ''"
        )
    }
}
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE user_stats ADD COLUMN yesterdayWords INT NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_stats ADD COLUMN themeColor INTEGER NOT NULL DEFAULT -10071900")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_stats ADD COLUMN isBackgroundEnabled INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create reading_materials table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `reading_materials` (
                `id` TEXT NOT NULL, 
                `title` TEXT NOT NULL, 
                `content` TEXT NOT NULL, 
                `difficultyLevel` INTEGER NOT NULL, 
                `isCalibrationMode` INTEGER NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        // Create comprehension_questions table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `comprehension_questions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `materialId` TEXT NOT NULL, 
                `questionText` TEXT NOT NULL, 
                `optionA` TEXT NOT NULL, 
                `optionB` TEXT NOT NULL, 
                `optionC` TEXT NOT NULL, 
                `optionD` TEXT NOT NULL, 
                `correctAnswerIndex` INTEGER NOT NULL, 
                FOREIGN KEY(`materialId`) REFERENCES `reading_materials`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_comprehension_questions_materialId` ON `comprehension_questions` (`materialId`)")

        // Create assessment_sessions table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `assessment_sessions` (
                `sessionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                `materialId` TEXT NOT NULL, 
                `initialWpm` INTEGER NOT NULL, 
                `finalWpm` INTEGER NOT NULL, 
                `totalReadingTimeMs` INTEGER NOT NULL, 
                `backtrackCount` INTEGER NOT NULL, 
                `correctAnswers` INTEGER NOT NULL, 
                `totalQuestions` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_stats ADD COLUMN optimalWpm INTEGER NOT NULL DEFAULT 250")
    }
}