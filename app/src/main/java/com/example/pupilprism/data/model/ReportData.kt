package com.example.pupilprism.data.model

data class StageRow(
    val nominalWpm: Int,
    val correct: Int,
    val total: Int,
    val effectiveWpm: Int,
    val backtracks: Int,
    val wpmChanges: Int,
    val condition: String
) {
    val accuracy: Float get() = if (total > 0) correct.toFloat() / total else 0f
}

data class ReportData(
    val participantId: String,
    val runId: String,
    val timestamp: Long,
    val optimalWpm: Int,
    val stages: List<StageRow>,
    val prosecnaNominalna: Int,
    val prosecnaEfektivna: Int,
    val gubitakProcenat: Int,
    val ukupnoReci: Int,
    val aktivnoVremeMs: Long,
    val ukupnoVracanja: Int,
    val ukupnoIzmenaBrzine: Int
) {
    val poslednjaIznadPraga: Int
        get() = stages.filter { it.accuracy >= 0.80f }.maxOfOrNull { it.nominalWpm } ?: 0
}
