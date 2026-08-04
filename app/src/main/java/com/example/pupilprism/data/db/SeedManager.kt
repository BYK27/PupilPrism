package com.example.pupilprism.data.db

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.pupilprism.data.model.ComprehensionQuestion
import com.example.pupilprism.data.model.ReadingMaterial
import com.example.pupilprism.data.model.SeedMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.security.MessageDigest

object SeedManager {

    private const val TAG = "SeedManager"
    private const val ASSET_NAME = "seed_assessment_data.json"

    suspend fun syncIfNeeded(
        context: Context,
        db: AppDatabase,
        force: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = context.assets.open(ASSET_NAME)
                .bufferedReader().use { it.readText() }
            val hash = sha256(json)

            val meta = db.seedMetaDao().get()
            if (!force && meta != null && meta.contentHash == hash) {
                Log.d(TAG, "Материјал непромењен (${meta.materialCount} текстова), прескачем.")
                return@withContext false
            }

            val broj = applySeed(db, json)
            db.seedMetaDao().set(
                SeedMeta(contentHash = hash, materialCount = broj)
            )
            Log.d(TAG, "Материјал учитан: $broj текстова. Отисак: ${hash.take(12)}")
            true
        } catch (e: Exception) {
            // Неуспех НЕ сме да обори апликацију: ако се материјал не учита,
            // самостално читање и даље мора да ради.
            Log.e(TAG, "Грешка при учитавању материјала", e)
            false
        }
    }

    private suspend fun applySeed(db: AppDatabase, json: String): Int {
        val dao = db.assessmentDao()
        val array = JSONArray(json)
        val idsUDatoteci = mutableSetOf<String>()

        db.withTransaction {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                // get(...).toString() ради и за број и за ниску,
                // па важи и за "id": 1 и за "id": "cal_01"
                val materialId = obj.get("id").toString()
                idsUDatoteci += materialId

                // ОБАВЕЗНО пре уписа: питања имају самогенеришући кључ,
                // па би их поновни упис удвостручио (видети одељак 1).
                dao.deleteQuestionsForMaterial(materialId)

                dao.insertMaterial(
                    ReadingMaterial(
                        id = materialId,
                        title = obj.optString("title", "Текст ${i + 1}"),
                        content = obj.getString("content"),
                        difficultyLevel = obj.optInt("difficultyLevel", 1),
                        isCalibrationMode = obj.getBoolean("isCalibration")
                    )
                )

                val qArray = obj.getJSONArray("questions")
                val pitanja = (0 until qArray.length()).map { j ->
                    val q = qArray.getJSONObject(j)
                    val opts = q.getJSONArray("options")
                    ComprehensionQuestion(
                        materialId = materialId,
                        questionText = q.getString("text"),
                        optionA = opts.getString(0),
                        optionB = opts.getString(1),
                        optionC = opts.getString(2),
                        optionD = opts.getString(3),
                        correctAnswerIndex = q.getInt("correctAnswerIndex")
                    )
                }
                dao.insertQuestions(pitanja)
            }

            val zaBrisanje = dao.getAllMaterialIds() - idsUDatoteci
            zaBrisanje.forEach { id ->
                dao.deleteQuestionsForMaterial(id)
                dao.deleteMaterial(id)
                Log.d(TAG, "Уклоњен текст који више није у датотеци: $id")
            }
        }
        return idsUDatoteci.size
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
