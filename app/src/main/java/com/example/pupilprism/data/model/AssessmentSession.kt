package com.example.pupilprism.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessment_sessions")
data class AssessmentSession(
    @PrimaryKey(autoGenerate = true) val sessionId: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val materialId: String,
    val initialWpm: Int,
    val finalWpm: Int,
    val totalReadingTimeMs: Long,
    val backtrackCount: Int,
    val correctAnswers: Int,
    val totalQuestions: Int
)