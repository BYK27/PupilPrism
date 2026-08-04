package com.example.pupilprism.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pupilprism.data.model.SeedMeta

@Dao
interface SeedMetaDao {
    @Query("SELECT * FROM seed_meta WHERE id = 1")
    suspend fun get(): SeedMeta?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(meta: SeedMeta)
}
