package com.example.pupilprism.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "seed_meta")
data class SeedMeta(
    @PrimaryKey val id: Int = 1,
    val contentHash: String,
    val appliedAt: Long = System.currentTimeMillis(),
    val materialCount: Int = 0
)
