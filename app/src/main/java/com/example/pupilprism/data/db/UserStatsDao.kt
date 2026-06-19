package com.example.pupilprism.data.db

import androidx.room.*
import com.example.pupilprism.data.model.UserStats

@Dao
interface UserStatsDao
{
    @Query("SELECT * FROM user_stats")
    suspend fun getStats(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStats)
}