package com.example.pupilprism.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pupilprism.data.model.AssessmentSession
import com.example.pupilprism.data.model.ComprehensionQuestion
import com.example.pupilprism.data.model.PdfBook
import com.example.pupilprism.data.model.ReadingMaterial
import com.example.pupilprism.data.model.UserStats
import com.example.speedreader.MIGRATION_1_2
import com.example.speedreader.MIGRATION_2_3
import com.example.speedreader.MIGRATION_3_4
import com.example.speedreader.MIGRATION_4_5
import com.example.speedreader.MIGRATION_5_6
import com.example.speedreader.MIGRATION_6_7
import com.example.speedreader.MIGRATION_7_8


@Database(
    entities = [
        PdfBook::class,
        UserStats::class,
        ReadingMaterial::class,
        ComprehensionQuestion::class,
        AssessmentSession::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pdfBookDao(): PdfBookDao
    abstract fun userStatsDao(): UserStatsDao

    abstract fun assessmentDao(): AssessmentDao
    // Inside your AppDatabase.kt file
    companion object {
        @Volatile
        public var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "speedreader-db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

