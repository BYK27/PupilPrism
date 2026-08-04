package com.example.pupilprism.data.model

data class PendingReadingSession(
    val materialId: String,
    val runId: String,
    val participantId: String,
    val condition: String,
    val initialWpm: Int,
    val finalWpm: Int,
    val wpmChangeCount: Int,
    val activeReadingTimeMs: Long,
    val totalElapsedTimeMs: Long,
    val wordsTotal: Int,
    val wordsConsumed: Int,
    val alpha: Double,
    val backtrackCount: Int
)

object SessionTelemetryStore {
    @Volatile
    private var pending: PendingReadingSession? = null
    @Volatile var runId: String = ""
    @Volatile var participantId: String = ""
    @Volatile var condition: String = ""

    fun put(session: PendingReadingSession) { pending = session }

    fun take(): PendingReadingSession? {
        val s = pending
        pending = null
        return s
    }

    fun clear() { pending = null }
}
