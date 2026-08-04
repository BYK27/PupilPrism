package com.example.pupilprism.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pupilprism.data.model.AssessmentSession
import com.example.pupilprism.data.model.ComprehensionQuestion
import com.example.pupilprism.data.model.PdfBook
import com.example.pupilprism.data.model.ReadingMaterial
import com.example.pupilprism.data.model.SeedMeta
import com.example.pupilprism.data.model.UserStats

@Database(
    entities = [
        PdfBook::class,
        UserStats::class,
        ReadingMaterial::class,
        ComprehensionQuestion::class,
        AssessmentSession::class,
        SeedMeta::class
    ],
    version = 11,
    exportSchema = true
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun pdfBookDao(): PdfBookDao
    abstract fun userStatsDao(): UserStatsDao

    abstract fun assessmentDao(): AssessmentDao

    abstract fun seedMetaDao(): SeedMetaDao
}

