package com.example.pupilprism.data.model

object ReadingTelemetry {

    private const val K_DUZINA = 0.5
    private const val MIN_DUZINA = 0.60
    private const val MAX_DUZINA = 1.80
    private const val PAUZA_RECENICA = 0.90
    private const val PAUZA_KLAUZA = 0.45
    private const val PAUZA_PASUS = 1.80
    private const val MIN_UKUPNO = 0.50
    private const val MAX_UKUPNO = 2.50

    const val MIN_TRAJANJE_MS = 55L

    private const val VODECI = "([{«\u201C\u2018\"'\u00BB"
    private const val ZAVRSNI = ")]}»\u201D\u2019\"'"
    private const val INTERPUNKCIJA = ".!?,;:\u2026\u2014-"

    const val OZNAKA_PASUSA = "\u00B6"

    fun jezgro(word: String): String =
        word.trim { it in VODECI || it in ZAVRSNI }
            .trimEnd { it in INTERPUNKCIJA }


    private fun zavrsniZnak(word: String): Char? =
        word.trimEnd { it in ZAVRSNI }.lastOrNull()


    fun referentnaDuzina(words: List<String>): Double {
        if (words.isEmpty()) return 5.0
        val duzine = words.map { jezgro(it).length }.filter { it > 0 }
        if (duzine.isEmpty()) return 5.0
        return duzine.average()
    }

    fun lengthFactor(word: String, lref: Double): Double {
        val l = jezgro(word).length.coerceAtLeast(1)
        val f = 1.0 + K_DUZINA * (l - lref) / lref
        return f.coerceIn(MIN_DUZINA, MAX_DUZINA)
    }

    fun pauseFactor(word: String): Double {
        if (word.endsWith(OZNAKA_PASUSA)) return PAUZA_PASUS
        return when (zavrsniZnak(word)) {
            '.', '!', '?', '\u2026' -> PAUZA_RECENICA
            ',', ';', ':' -> PAUZA_KLAUZA
            else -> 0.0
        }
    }

    fun wordFactor(word: String, lref: Double): Double =
        (lengthFactor(word, lref) + pauseFactor(word))
            .coerceIn(MIN_UKUPNO, MAX_UKUPNO)

    fun wordDurationMicros(
        word: String,
        wpm: Int,
        adaptive: Boolean,
        lref: Double = 5.0,
        alpha: Double = 0.0
    ): Long {
        val base = 60_000_000.0 / wpm.coerceAtLeast(1)
        if (!adaptive) return base.toLong().coerceAtLeast(MIN_TRAJANJE_MS * 1000)
        var d = base * wordFactor(word, lref)
        if (alpha > 0.0) d /= alpha
        return d.toLong().coerceAtLeast(MIN_TRAJANJE_MS * 1000)
    }

    fun computeAlpha(words: List<String>): Double {
        if (words.isEmpty()) return 1.0
        val lref = referentnaDuzina(words)
        return words.sumOf { wordFactor(it, lref) } / words.size
    }

    fun predictedEffectiveWpm(nominalWpm: Int, words: List<String>): Double =
        nominalWpm / computeAlpha(words)

    fun segmentiraj(raw: String): List<String> {
        return raw
            .replace(Regex("\\n\\s*\\n+"), " ${ReadingTelemetry.OZNAKA_PASUSA} ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .let { reci ->
                val out = mutableListOf<String>()
                reci.forEach { w ->
                    if (w == ReadingTelemetry.OZNAKA_PASUSA && out.isNotEmpty()) {
                        out[out.lastIndex] = out.last() + ReadingTelemetry.OZNAKA_PASUSA
                    } else if (w != ReadingTelemetry.OZNAKA_PASUSA) {
                        out += w
                    }
                }
                out
            }
    }

}

