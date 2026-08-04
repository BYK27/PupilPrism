package com.example.pupilprism.data.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pupilprism.Config
import com.example.pupilprism.data.db.AssessmentDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RSVPViewModel(
    private val assessmentDao: AssessmentDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(RSVPState())
    val uiState: StateFlow<RSVPState> = _uiState.asStateFlow()

    private val _stats = MutableStateFlow(ReadingStats())
    val stats: StateFlow<ReadingStats> = _stats.asStateFlow()

    private var readingJob: Job? = null
    private var statsJob: Job? = null

    // --- време ---
    private var sessionStartTimeMs: Long = 0L   // тренутак учитавања текста
    private var lastResumeTimeMs: Long = 0L     // тренутак последњег наставка

    var activeReadingTimeMs: Long = 0L          // само време приказа, без пауза
        private set

    // --- положај ---
    private var pocetniIndeks: Int = 0
    private var maxIndexReached: Int = 0

    // --- бројачи ---
    var backtrackCount: Int = 0
        private set
    var wpmChangeCount: Int = 0
        private set
    var initialWpm: Int = 0
        private set

    // --- параметри текста ---
    var lref: Double = 5.0
        private set
    var alpha: Double = 1.0
        private set

    // --- временски пондерисан просек номиналне брзине ---
    private var wpmIntegral: Double = 0.0
    private var wpmIntegralTimeMs: Long = 0L
    private var lastWpmSampleMs: Long = 0L

    // --- ознаке експеримента ---
    private var materialId: String = ""
    var runId: String = ""
    var participantId: String = ""
    var condition: String = ""
    private var optimalWpm: Int = 0

    /** Поставља екран, из профила корисника, ради упозорења о прекорачењу. */
    fun setOptimalWpm(value: Int) { optimalWpm = value }

    // ------------------------------------------------------------------
    // Изведене величине
    // ------------------------------------------------------------------

    /** Укупно протекло време од учитавања текста, укључујући паузе. */
    val totalElapsedTimeMs: Long
        get() = if (sessionStartTimeMs == 0L) 0L
        else System.currentTimeMillis() - sessionStartTimeMs

    /**
     * Број РАЗЛИЧИТИХ речи кроз које је корисник прошао.
     * Рачуна се из НАЈВЕЋЕГ достигнутог индекса, а не из текућег: после враћања
     * уназад текући индекс може бити мањи, па би број прочитаних речи испао мањи
     * него што јесте. Време поновног читања и даље улази у именилац, што и производи
     * разлику између номиналне и ефективне брзине.
     */
    val wordsConsumed: Int
        get() = (maxIndexReached - pocetniIndeks + 1).coerceAtLeast(0)

    private fun tekuciIntervalMs(): Long =
        if (lastResumeTimeMs > 0L) System.currentTimeMillis() - lastResumeTimeMs else 0L

    /** Стварна брзина: прочитане речи по минуту активног времена. */
    val efektivnaWpm: Double
        get() {
            val aktivno = activeReadingTimeMs + tekuciIntervalMs()
            return if (aktivno > 0) wordsConsumed * 60000.0 / aktivno else 0.0
        }

    val sesijskaWpm: Double
        get() = AssessmentSession.computeWpm(wordsConsumed, totalElapsedTimeMs)

    // ------------------------------------------------------------------
    // Учитавање
    // ------------------------------------------------------------------

    fun loadContent(
        text: String,
        id: String,
        startWpm: Int,
        isCalibration: Boolean,
        startFrom: Int = 0
    ) {
        val reci = ReadingTelemetry.segmentiraj(text)
        materialId = id
        initialWpm = startWpm

        val pocetak = startFrom.coerceIn(0, maxOf(0, reci.size - 1))

        _uiState.update {
            it.copy(
                words = reci,
                currentIndex = pocetak,
                wpm = startWpm,
                isPaused = true,
                isFinished = false,
                isCalibrationMode = isCalibration,
                isControlsLocked = isCalibration
            )
        }

        // ресетовање телеметрије
        activeReadingTimeMs = 0L
        backtrackCount = 0
        wpmChangeCount = 0
        lastResumeTimeMs = 0L
        pocetniIndeks = pocetak
        maxIndexReached = pocetak

        // ВАЖНО: lref пре alpha, зато што alpha зависи од lref
        lref = ReadingTelemetry.referentnaDuzina(reci)
        alpha = ReadingTelemetry.computeAlpha(reci)

        wpmIntegral = 0.0
        wpmIntegralTimeMs = 0L
        lastWpmSampleMs = 0L

        sessionStartTimeMs = System.currentTimeMillis()
        _stats.value = ReadingStats()
    }

    fun loadMaterialFromDb(materialId: String, startWpm: Int, isCalibration: Boolean) {
        viewModelScope.launch {
            assessmentDao.getMaterialById(materialId)?.let {
                loadContent(it.content, materialId, startWpm, isCalibration)
            }
        }
    }

    // ------------------------------------------------------------------
    // Петља приказа
    // ------------------------------------------------------------------

    fun togglePause() {
        val cur = _uiState.value
        if (cur.isFinished || cur.words.isEmpty()) return

        if (!cur.isPaused) {
            sampleWpm()                                  // затвара интервал старе брзине
            _uiState.update { it.copy(isPaused = true) }
            stopReadingLoop()
        } else {
            _uiState.update { it.copy(isPaused = false) }
            lastWpmSampleMs = System.currentTimeMillis()
            startReadingLoop()
        }
    }

    private fun startReadingLoop() {
        lastResumeTimeMs = System.currentTimeMillis()
        readingJob?.cancel()
        startStatsTicker()

        readingJob = viewModelScope.launch {
            // Циљни тренуци се сабирају УНАПРЕД, да кашњење једне итерације не би
            // померало наредне. delay() гарантује најмање задато време и по правилу
            // враћа контролу нешто касније; без компензације тај вишак се сабира
            // кроз стотине речи.
            var nextSwitchNanos = System.nanoTime()

            while (isActive && !_uiState.value.isPaused) {
                val state = _uiState.value

                if (state.currentIndex >= state.words.size - 1) {
                    finishReading()
                    break
                }

                val micros = ReadingTelemetry.wordDurationMicros(
                    word = state.words[state.currentIndex],
                    wpm = state.wpm,
                    adaptive = state.isAdaptiveSpeedEnabled,
                    lref = lref,
                    alpha = if (state.isNormalizedAdaptive) alpha else 0.0
                )
                nextSwitchNanos += micros * 1_000L

                val sleepMillis = (nextSwitchNanos - System.nanoTime()) / 1_000_000L
                if (sleepMillis > 0L) {
                    delay(sleepMillis)
                } else {
                    // Систем је заостао. Не надокнађујемо прескакањем речи, јер би то
                    // била реч коју корисник никада није видео.
                    nextSwitchNanos = System.nanoTime()
                }

                _uiState.update {
                    val next = it.currentIndex + 1
                    if (next > maxIndexReached) maxIndexReached = next
                    it.copy(currentIndex = next)
                }
            }
        }
    }

    private fun stopReadingLoop() {
        readingJob?.cancel()
        statsJob?.cancel()
        if (lastResumeTimeMs > 0L) {
            activeReadingTimeMs += System.currentTimeMillis() - lastResumeTimeMs
            lastResumeTimeMs = 0L
        }
        osveziStats()
    }

    private fun finishReading() {
        sampleWpm()
        stopReadingLoop()
        _uiState.update { it.copy(isFinished = true, isPaused = true) }

        SessionTelemetryStore.put(
            PendingReadingSession(
                materialId = materialId,
                runId = runId,
                participantId = participantId,
                condition = condition,
                initialWpm = initialWpm,
                finalWpm = _uiState.value.wpm,
                wpmChangeCount = wpmChangeCount,
                activeReadingTimeMs = activeReadingTimeMs,
                totalElapsedTimeMs = totalElapsedTimeMs,
                wordsTotal = _uiState.value.words.size,
                wordsConsumed = wordsConsumed,
                alpha = alpha,
                backtrackCount = backtrackCount
            )
        )
    }

    // ------------------------------------------------------------------
    // Контроле
    // ------------------------------------------------------------------

    fun changeWpm(delta: Int) {
        if (_uiState.value.isControlsLocked) return
        sampleWpm()
        _uiState.update {
            val novi = (it.wpm + delta).coerceIn(Config.MIN_WPM, Config.MAX_WPM)
            if (novi != it.wpm) wpmChangeCount++
            it.copy(wpm = novi)
        }
    }

    fun backtrack() {
        if (_uiState.value.isControlsLocked) return
        _uiState.update {
            if (it.currentIndex > 0) {
                backtrackCount++
                it.copy(currentIndex = it.currentIndex - 1, isFinished = false)
            } else it
        }
    }


    fun forward() {
        if (_uiState.value.isControlsLocked) return
        _uiState.update {
            if (it.currentIndex < it.words.size - 1) {
                val next = it.currentIndex + 1
                if (next > maxIndexReached) maxIndexReached = next
                it.copy(currentIndex = next)
            } else it
        }
    }

    fun jumpToIndex(index: Int) {
        if (_uiState.value.isControlsLocked) return
        _uiState.update {
            val cilj = index.coerceIn(0, maxOf(0, it.words.size - 1))
            if (cilj < it.currentIndex) backtrackCount++
            if (cilj > maxIndexReached) maxIndexReached = cilj

            val josNaKraju = cilj >= it.words.size - 1

            it.copy(currentIndex = cilj, isFinished = it.isFinished && josNaKraju)
        }
    }


    fun toggleAdaptiveSpeed() {
        _uiState.update { it.copy(isAdaptiveSpeedEnabled = !it.isAdaptiveSpeedEnabled) }
    }

    fun toggleProgressDisplayMode() {
        _uiState.update {
            it.copy(
                displayMode = if (it.displayMode == ProgressDisplayMode.PERCENTAGE)
                    ProgressDisplayMode.FRACTION else ProgressDisplayMode.PERCENTAGE
            )
        }
    }

    fun toggleMultiWordMode() {
        _uiState.update { it.copy(isMultiWordMode = !it.isMultiWordMode) }
    }

    fun setOrpEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isOrpEnabled = enabled) }
    }

    // ------------------------------------------------------------------
    // Просечна брзина и статистика
    // ------------------------------------------------------------------

    /** Затвара текући интервал брзине и додаје га интегралу. */
    private fun sampleWpm() {
        val now = System.currentTimeMillis()
        if (lastWpmSampleMs > 0L && !_uiState.value.isPaused) {
            val dt = now - lastWpmSampleMs
            wpmIntegral += _uiState.value.wpm.toDouble() * dt
            wpmIntegralTimeMs += dt
        }
        lastWpmSampleMs = now
    }

    /** Просек који урачунава и текући, још незатворени интервал. */
    private fun prosecnaPodesena(): Double {
        val dt = if (lastWpmSampleMs > 0L && !_uiState.value.isPaused)
            System.currentTimeMillis() - lastWpmSampleMs else 0L
        val ukupno = wpmIntegralTimeMs + dt
        if (ukupno <= 0L) return _uiState.value.wpm.toDouble()
        return (wpmIntegral + _uiState.value.wpm.toDouble() * dt) / ukupno
    }

    private fun startStatsTicker() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            while (isActive) {
                delay(Config.TIKER_STATISTIKE_MS)
                osveziStats()
            }
        }
    }

    private fun osveziStats() {
        val s = _uiState.value
        val prosek = prosecnaPodesena()
        val efektivna = efektivnaWpm
        val preostale = (s.words.size - s.currentIndex).coerceAtLeast(0)
        val zaProcenu = if (efektivna > 20) efektivna else s.wpm.toDouble()

        _stats.value = ReadingStats(
            prosecnaPodesenaWpm = prosek.toInt(),
            efektivnaWpm = efektivna.toInt(),
            gubitakProcenat = if (prosek > 0)
                ((prosek - efektivna) / prosek * 100).toInt().coerceIn(0, 99) else 0,
            preostaloSekundi = (preostale * 60.0 / zaProcenu).toInt(),
            // у режиму испитивања упозорење се не приказује, било би сугестија
            iznadKalibrisane = !s.isCalibrationMode && optimalWpm > 0 && s.wpm > optimalWpm
        )
    }

    override fun onCleared() {
        super.onCleared()
        readingJob?.cancel()
        statsJob?.cancel()
    }

    fun setContextRadius(value: Int) {
        _uiState.update {
            it.copy(contextRadius = value.coerceIn(Config.KONTEKST_MIN, Config.KONTEKST_MAX))
        }
    }

}