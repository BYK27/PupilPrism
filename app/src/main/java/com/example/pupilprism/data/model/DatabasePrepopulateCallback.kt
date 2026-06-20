package com.example.pupilprism.data.db

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pupilprism.data.model.ComprehensionQuestion
import com.example.pupilprism.data.model.ReadingMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader

class DatabasePrepopulateCallback(
    private val context: Context,
    private val scope: CoroutineScope
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        // This is executed only once, when the database is first created.
        scope.launch(Dispatchers.IO) {
            prePopulateDatabase()
        }
    }

    private suspend fun prePopulateDatabase() {
        try {
            // 1. Read JSON from assets
            val inputStream = context.assets.open("seed_assessment_data.json")
            val jsonString = inputStream.bufferedReader().use(BufferedReader::readText)

            val jsonObject = JSONObject(jsonString)

            // Get our DAO from the AppDatabase instance
            // Note: In a real DI setup (Hilt/Dagger), you'd inject the DAO directly.
            // Since you are instantiating Room in MainActivity, we need to fetch it.
            val database = AppDatabase.getInstance(context) // Ensure you have a getInstance() method
            val assessmentDao = database.assessmentDao()

            // 2. Parse and Insert Reading Materials
            val materialsArray = jsonObject.getJSONArray("reading_materials")
            for (i in 0 until materialsArray.length()) {
                val matObj = materialsArray.getJSONObject(i)
                val material = ReadingMaterial(
                    id = matObj.getString("id"),
                    title = matObj.getString("title"),
                    content = matObj.getString("content"),
                    difficultyLevel = matObj.getInt("difficultyLevel"),
                    isCalibrationMode = matObj.getInt("isCalibrationMode") == 1
                )
                assessmentDao.insertMaterial(material)
            }

            // 3. Parse and Insert Comprehension Questions
            val questionsArray = jsonObject.getJSONArray("comprehension_questions")
            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(i)
                val question = ComprehensionQuestion(
                    materialId = qObj.getString("materialId"),
                    questionText = qObj.getString("questionText"),
                    optionA = qObj.getString("optionA"),
                    optionB = qObj.getString("optionB"),
                    optionC = qObj.getString("optionC"),
                    optionD = qObj.getString("optionD"),
                    correctAnswerIndex = qObj.getInt("correctAnswerIndex")
                )
                assessmentDao.insertQuestion(question)
            }

            Log.d("DatabasePrepopulate", "Successfully seeded database with calibration materials.")

        } catch (e: Exception) {
            Log.e("DatabasePrepopulate", "Error seeding database", e)
        }
    }
}