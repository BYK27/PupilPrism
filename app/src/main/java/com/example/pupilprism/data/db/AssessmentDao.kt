package com.example.pupilprism.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.pupilprism.data.model.AssessmentSession
import com.example.pupilprism.data.model.ComprehensionQuestion
import com.example.pupilprism.data.model.ReadingMaterial
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: ReadingMaterial)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<ComprehensionQuestion>)

    @Insert
    suspend fun insertSession(session: AssessmentSession)

    @Query("SELECT * FROM reading_materials WHERE isCalibrationMode = :isCalibration")
    fun getMaterialsByMode(isCalibration: Boolean): Flow<List<ReadingMaterial>>

    @Query("SELECT * FROM comprehension_questions WHERE materialId = :materialId")
    suspend fun getQuestionsForMaterial(materialId: String): List<ComprehensionQuestion>

    @Query("SELECT * FROM assessment_sessions ORDER BY timestamp DESC")
    suspend fun getAllSessions(): List<AssessmentSession>

    // Add this missing method!
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: ComprehensionQuestion)

    @Query("SELECT * FROM reading_materials WHERE id = :materialId")
    suspend fun getMaterialById(materialId: String): ReadingMaterial?

    @Query("SELECT id FROM reading_materials")
    suspend fun getAllMaterialIds(): List<String>

    @Query("DELETE FROM comprehension_questions WHERE materialId = :materialId")
    suspend fun deleteQuestionsForMaterial(materialId: String)

    @Query("DELETE FROM reading_materials WHERE id = :materialId")
    suspend fun deleteMaterial(materialId: String)

    @Query("SELECT COUNT(*) FROM comprehension_questions WHERE materialId = :materialId")
    suspend fun countQuestionsForMaterial(materialId: String): Int

    @Query("SELECT * FROM assessment_sessions WHERE runId = :runId ORDER BY timestamp ASC")
    suspend fun getSessionsForRun(runId: String): List<AssessmentSession>

    @Query("SELECT DISTINCT runId FROM assessment_sessions WHERE runId != '' ORDER BY runId DESC LIMIT 20")
    suspend fun getRecentRunIds(): List<String>

    @Query("SELECT COUNT(*) FROM assessment_sessions")
    suspend fun countSessions(): Int


}