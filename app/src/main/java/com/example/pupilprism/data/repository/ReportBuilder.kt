package com.example.pupilprism.data.repository

import com.example.pupilprism.data.db.AssessmentDao
import com.example.pupilprism.data.model.ReportData
import com.example.pupilprism.data.model.StageRow

object ReportBuilder {

    suspend fun build(
        dao: AssessmentDao,
        runId: String,
        optimalWpm: Int
    ): ReportData? {
        val sesije = dao.getSessionsForRun(runId)
        if (sesije.isEmpty()) return null

        val stages = sesije
            .sortedBy { it.finalWpm }
            .map {
                StageRow(
                    nominalWpm = it.finalWpm,
                    correct = it.correctAnswers,
                    total = it.totalQuestions,
                    effectiveWpm = it.effectiveWpm.toInt(),
                    backtracks = it.backtrackCount
                )
            }

        val nominalna = sesije.map { it.finalWpm }.average()
        val efektivna = sesije.map { it.effectiveWpm }.filter { it > 0 }.average()
        val gubitak = if (nominalna > 0) ((nominalna - efektivna) / nominalna * 100) else 0.0

        return ReportData(
            participantId = sesije.first().participantId.ifBlank { "-" },
            runId = runId,
            timestamp = sesije.last().timestamp,
            optimalWpm = optimalWpm,
            stages = stages,
            prosecnaNominalna = nominalna.toInt(),
            prosecnaEfektivna = if (efektivna.isNaN()) 0 else efektivna.toInt(),
            gubitakProcenat = gubitak.toInt().coerceIn(0, 99),
            ukupnoReci = sesije.sumOf { it.wordsConsumed },
            aktivnoVremeMs = sesije.sumOf { it.activeReadingTimeMs },
            ukupnoVracanja = sesije.sumOf { it.backtrackCount },
            ukupnoIzmenaBrzine = sesije.sumOf { it.wpmChangeCount }
        )
    }
}
