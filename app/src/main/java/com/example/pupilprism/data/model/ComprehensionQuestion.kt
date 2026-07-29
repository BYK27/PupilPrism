package com.example.pupilprism.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "comprehension_questions",
    foreignKeys = [
        ForeignKey(
            entity = ReadingMaterial::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["materialId"])]
)
data class ComprehensionQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val materialId: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswerIndex: Int
)