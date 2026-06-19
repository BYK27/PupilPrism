package com.example.pupilprism.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_materials")
data class ReadingMaterial(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val difficultyLevel: Int, // e.g., 1 for easy, 3 for hard
    val isCalibrationMode: Boolean // True for Phase 1, False for Phase 2
)