package com.example.pupilprism.data.model

enum class ProgressDisplayMode {
    PERCENTAGE,
    FRACTION
}

data class RSVPState(
    val words: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val wpm: Int = 250,
    val isPaused: Boolean = true,
    val isAdaptiveSpeedEnabled: Boolean = false,
    val isFinished: Boolean = false,
    val isCalibrationMode: Boolean = false,
    val displayMode: ProgressDisplayMode = ProgressDisplayMode.PERCENTAGE,
    val isMultiWordMode: Boolean = false,
    val isControlsLocked: Boolean = false,
    val isNormalizedAdaptive: Boolean = false,
    val isOrpEnabled: Boolean = true,
    val contextRadius: Int = 3
) {
    val currentWord: String
        get() = if (words.isNotEmpty() && currentIndex < words.size) words[currentIndex] else ""

    val progressPercentage: Float
        get() = if (words.isNotEmpty()) currentIndex.toFloat() / words.size else 0f

    val remainingWords: Int
        get() = maxOf(0, words.size - currentIndex)
}

data class ReadingStats(
    val prosecnaPodesenaWpm: Int = 0,
    val efektivnaWpm: Int = 0,
    val gubitakProcenat: Int = 0,
    val preostaloSekundi: Int = 0,
    val iznadKalibrisane: Boolean = false
)
