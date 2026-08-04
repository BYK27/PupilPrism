package com.example.pupilprism.data.db

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pupilprism.data.model.ComprehensionQuestion
import com.example.pupilprism.data.model.ReadingMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader

class DatabasePrepopulateCallback(
    private val context: Context,
    private val scope: CoroutineScope
) : RoomDatabase.Callback() {

    /*
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        Log.d("DatabasePrepopulate", "onCreate triggered. Seeding data...")

        scope.launch {
            try {
                // 1. Read JSON from assets
                val inputStream = context.assets.open("seed_assessment_data.json")
                val jsonString = inputStream.bufferedReader().use(BufferedReader::readText)

                // 2. Parse the new flat JSON array
                val jsonArray = JSONArray(jsonString)

                val database = AppDatabase.getInstance(context)
                val assessmentDao = database.assessmentDao()

                // 3. Loop through each material
                for (i in 0 until jsonArray.length()) {
                    val matObj = jsonArray.getJSONObject(i)
                    val materialId = matObj.getString("id")

                    // Insert the ReadingMaterial
                    val material = ReadingMaterial(
                        id = materialId,
                        title = "Assessment Phase ${i + 1}", // Title generated dynamically as it was removed from JSON
                        content = matObj.getString("content"),
                        difficultyLevel = i + 1, // Scaling difficulty based on index
                        isCalibrationMode = matObj.getBoolean("isCalibration")
                    )
                    assessmentDao.insertMaterial(material)

                    // 4. Parse the inner nested array of questions for this specific material
                    val questionsArray = matObj.getJSONArray("questions")
                    for (j in 0 until questionsArray.length()) {
                        val qObj = questionsArray.getJSONObject(j)
                        val optionsArray = qObj.getJSONArray("options")

                        val question = ComprehensionQuestion(
                            materialId = materialId,
                            questionText = qObj.getString("text"),
                            optionA = optionsArray.getString(0),
                            optionB = optionsArray.getString(1),
                            optionC = optionsArray.getString(2),
                            optionD = optionsArray.getString(3),
                            correctAnswerIndex = qObj.getInt("correctAnswerIndex")
                        )
                        assessmentDao.insertQuestion(question)
                    }
                }

                Log.d("DatabasePrepopulate", "Successfully seeded database with calibration materials.")
            } catch (e: Exception) {
                Log.e("DatabasePrepopulate", "Error seeding database", e)
            }
        }
    }
    */
}