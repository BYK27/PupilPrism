package com.example.pupilprism.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        db.execSQL("ALTER TABLE user_stats ADD COLUMN streakUpdatedDate TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_stats ADD COLUMN yesterdayWords INT NOT NULL DEFAULT 0")
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
                FOREIGN KEY(`materialId`) REFERENCES `reading_materials`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_comprehension_questions_materialId` " +
                "ON `comprehension_questions` (`materialId`)")

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

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `assessment_sessions_new` (
                `sessionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `materialId` TEXT NOT NULL,
                `participantId` TEXT NOT NULL DEFAULT '',
                `condition` TEXT NOT NULL DEFAULT '',
                `initialWpm` INTEGER NOT NULL,
                `finalWpm` INTEGER NOT NULL,
                `wpmChangeCount` INTEGER NOT NULL DEFAULT 0,
                `activeReadingTimeMs` INTEGER NOT NULL,
                `totalElapsedTimeMs` INTEGER NOT NULL DEFAULT 0,
                `wordsTotal` INTEGER NOT NULL DEFAULT 0,
                `wordsConsumed` INTEGER NOT NULL DEFAULT 0,
                `effectiveWpm` REAL NOT NULL DEFAULT 0.0,
                `sessionWpm` REAL NOT NULL DEFAULT 0.0,
                `alpha` REAL NOT NULL DEFAULT 0.0,
                `algorithmVersion` TEXT NOT NULL DEFAULT 'adaptive-v1',
                `backtrackCount` INTEGER NOT NULL,
                `correctAnswers` INTEGER NOT NULL,
                `totalQuestions` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `assessment_sessions_new`
                (sessionId, timestamp, materialId, initialWpm, finalWpm,
                 activeReadingTimeMs, totalElapsedTimeMs, backtrackCount,
                 correctAnswers, totalQuestions)
            SELECT sessionId, timestamp, materialId, initialWpm, finalWpm,
                   totalReadingTimeMs, totalReadingTimeMs, backtrackCount,
                   correctAnswers, totalQuestions
            FROM `assessment_sessions`
        """.trimIndent())

        db.execSQL("DROP TABLE `assessment_sessions`")
        db.execSQL("ALTER TABLE `assessment_sessions_new` RENAME TO `assessment_sessions`")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `seed_meta` (
                `id` INTEGER NOT NULL PRIMARY KEY,
                `contentHash` TEXT NOT NULL,
                `appliedAt` INTEGER NOT NULL,
                `materialCount` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_stats ADD COLUMN isOrpEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_stats ADD COLUMN isOrpGuidesEnabled INTEGER NOT NULL DEFAULT 1")
    }
}

/** Једно место на коме се ланац одржава. */
val SVE_MIGRACIJE = arrayOf(
    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
    MIGRATION_9_10, MIGRATION_10_11
)
