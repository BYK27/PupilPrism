package com.example.pupilprism.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessment_sessions")
data class AssessmentSession(
    @PrimaryKey(autoGenerate = true) val sessionId: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val materialId: String,

    val participantId: String = "",
    val condition: String = "",

    val initialWpm: Int,
    val finalWpm: Int,
    val wpmChangeCount: Int = 0,

    val activeReadingTimeMs: Long,
    val totalElapsedTimeMs: Long,

    val wordsTotal: Int = 0,
    val wordsConsumed: Int = 0,

    val effectiveWpm: Double = 0.0,
    val sessionWpm: Double = 0.0,
    val alpha: Double = 0.0,

    val backtrackCount: Int,
    val correctAnswers: Int,
    val totalQuestions: Int,

    val algorithmVersion: String = "adaptive-v2",
) {
    val comprehension: Double
        get() = if (totalQuestions > 0) correctAnswers.toDouble() / totalQuestions else 0.0

    val readingEfficiency: Double
        get() = effectiveWpm * comprehension

    val speedLossPercent: Double
        get() = if (finalWpm > 0) (finalWpm - effectiveWpm) / finalWpm * 100.0 else 0.0

    companion object {
        fun computeWpm(words: Int, millis: Long): Double =
            if (millis > 0L) words * 60000.0 / millis else 0.0
    }
}

