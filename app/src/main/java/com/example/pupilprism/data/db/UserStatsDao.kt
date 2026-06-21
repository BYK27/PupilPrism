package com.example.pupilprism.data.db

import androidx.room.*
import com.example.pupilprism.data.model.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao
{
    @Query("SELECT * FROM user_stats")
    suspend fun getStats(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStats)

    // Add this reactive Flow query
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getStatsFlow(): Flow<UserStats?>
}