package com.example.pupilprism.data.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.pupilprism.data.model.ReportData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object PdfReportGenerator {

    private const val SIRINA = 595   // A4 при 72 тачке по инчу
    private const val VISINA = 842
    private const val MARGINA = 48f

    private val CRNA = Color.parseColor("#1A1A1A")
    private val SIVA = Color.parseColor("#6B6B6B")
    private val PLAVA = Color.parseColor("#1F4E79")
    private val CRVENA = Color.parseColor("#C0392B")
    private val SVETLA = Color.parseColor("#E8E8E8")

    fun generate(context: Context, r: ReportData, ime: String?): File {
        val doc = PdfDocument()
        val page = doc.startPage(
            PdfDocument.PageInfo.Builder(SIRINA, VISINA, 1).create()
        )
        val c = page.canvas
        var y = MARGINA

        val naslov = paint(20f, PLAVA, true)
        val podnaslov = paint(11f, SIVA)
        val telo = paint(10f, CRNA)
        val malo = paint(8.5f, SIVA)
        val ogromno = paint(44f, PLAVA, true)
        val zaglavlje = paint(9.5f, CRNA, true)

        // --- заглавље ---
        c.drawText("Извештај о процени брзине читања", MARGINA, y + 20f, naslov)
        y += 34f

        val datum = SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale("sr")).format(Date(r.timestamp))
        c.drawText(datum, MARGINA, y, podnaslov)
        y += 14f

        if (!ime.isNullOrBlank()) {
            c.drawText("Учесник: ${ime.trim()}", MARGINA, y, telo)
            y += 14f
        }
        c.drawText("Шифра: ${r.participantId}", MARGINA, y, podnaslov)
        y += 22f

        linija(c, y); y += 24f

        // --- главни резултат ---
        c.drawText("Ваша брзина читања уз очувано разумевање", MARGINA, y, zaglavlje)
        y += 44f
        c.drawText("${r.optimalWpm}", MARGINA, y, ogromno)
        val sirinaBroja = ogromno.measureText("${r.optimalWpm}")
        c.drawText("речи у минути", MARGINA + sirinaBroja + 10f, y, podnaslov)
        y += 20f
        c.drawText(
            "То је највећа брзина на којој сте тачно одговорили на најмање 80 % питања.",
            MARGINA, y, malo
        )
        y += 28f

        // --- крива калибрације ---
        c.drawText("Тачност одговора по брзинама", MARGINA, y, zaglavlje)
        y += 12f
        y = crtajGrafik(c, r, y) + 24f

        // --- табела ---
        c.drawText("Резултат по етапама", MARGINA, y, zaglavlje)
        y += 16f
        y = crtajTabelu(c, r, y) + 24f

        // --- номинално наспрам стварног ---
        c.drawText("Подешена наспрам стварне брзине", MARGINA, y, zaglavlje)
        y += 16f
        c.drawText(
            "Просечно подешено: ${r.prosecnaNominalna} WPM     " +
                    "Стварно прочитано: ${r.prosecnaEfektivna} WPM     " +
                    "Разлика: ${r.gubitakProcenat} %",
            MARGINA, y, telo
        )
        y += 14f
        c.drawText(
            "Разлика настаје зато што апликација дужим речима и крајевима реченица " +
                    "даје нешто више времена.",
            MARGINA, y, malo
        )
        y += 26f

        // --- понашање ---
        c.drawText("Ток читања", MARGINA, y, zaglavlje)
        y += 16f
        val minuta = r.aktivnoVremeMs / 60000.0
        c.drawText(
            "Прочитано речи: ${r.ukupnoReci}     " +
                    "Време читања: ${"%.1f".format(Locale.ROOT, minuta)} min     " +
                    "Враћања уназад: ${r.ukupnoVracanja}     " +
                    "Измена брзине: ${r.ukupnoIzmenaBrzine}",
            MARGINA, y, telo
        )
        y += 30f

        // --- напомена ---
        linija(c, VISINA - 72f)
        c.drawText(
            "Ово није дијагностички тест. Резултат зависи од врсте текста, дневне " +
                    "форме и навике на овакав приказ,",
            MARGINA, VISINA - 54f, malo
        )
        c.drawText(
            "и може се разликовати између мерења. Извештај је намењен искључиво " +
                    "учеснику.",
            MARGINA, VISINA - 42f, malo
        )

        doc.finishPage(page)

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        // старији извештаји се бришу: у кешу не треба да остају документи са именима
        dir.listFiles()?.forEach { if (it.isFile) it.delete() }

        val naziv = "izvestaj_${r.participantId}_" +
                SimpleDateFormat("yyyyMMdd_HHmm", Locale.ROOT).format(Date(r.timestamp)) + ".pdf"
        val file = File(dir, naziv)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    // ------------------------------------------------------------------

    private fun paint(velicina: Float, boja: Int, podebljano: Boolean = false) = Paint().apply {
        isAntiAlias = true
        textSize = velicina
        color = boja
        typeface = if (podebljano)
            android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        else android.graphics.Typeface.DEFAULT
    }

    private fun linija(c: Canvas, y: Float) {
        c.drawLine(MARGINA, y, SIRINA - MARGINA, y, Paint().apply {
            color = SVETLA
            strokeWidth = 1f
        })
    }

    /** Тачност по брзинама, са прагом од 80 %. Враћа доњу ивицу графика. */
    private fun crtajGrafik(c: Canvas, r: ReportData, top: Float): Float {
        val visina = 150f
        val levo = MARGINA + 30f
        val desno = SIRINA - MARGINA
        val dno = top + visina

        val osa = Paint().apply { color = SIVA; strokeWidth = 1f; isAntiAlias = true }
        val mreza = Paint().apply { color = SVETLA; strokeWidth = 1f }
        val oznaka = paint(7.5f, SIVA)

        // водоравне линије на 0, 20, ..., 100 %
        for (p in 0..100 step 20) {
            val yy = dno - visina * p / 100f
            c.drawLine(levo, yy, desno, yy, mreza)
            c.drawText("$p%", MARGINA - 4f, yy + 3f, oznaka)
        }

        // праг
        val yPrag = dno - visina * 0.8f
        c.drawLine(levo, yPrag, desno, yPrag, Paint().apply {
            color = CRVENA
            strokeWidth = 1.5f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 4f), 0f)
        })

        c.drawLine(levo, dno, desno, dno, osa)

        if (r.stages.isEmpty()) return dno + 14f

        val korak = if (r.stages.size > 1) (desno - levo) / (r.stages.size - 1) else 0f
        val linijaP = Paint().apply { color = PLAVA; strokeWidth = 2.5f; isAntiAlias = true }
        val tackaP = Paint().apply { color = PLAVA; isAntiAlias = true }

        var prethX = 0f
        var prethY = 0f
        r.stages.forEachIndexed { i, s ->
            val x = levo + korak * i
            val yy = dno - visina * s.accuracy
            if (i > 0) c.drawLine(prethX, prethY, x, yy, linijaP)
            c.drawCircle(x, yy, 4f, tackaP)
            c.drawText("${s.nominalWpm}", x - 10f, dno + 12f, oznaka)
            prethX = x; prethY = yy
        }

        c.drawText("брзина приказа (WPM)", desno - 90f, dno + 24f, oznaka)
        return dno + 26f
    }

    private fun crtajTabelu(c: Canvas, r: ReportData, top: Float): Float {
        val zaglavljeP = paint(8.5f, CRNA, true)
        val celijaP = paint(8.5f, CRNA)
        val kolone = floatArrayOf(0f, 90f, 170f, 250f, 350f, 440f)
        var y = top

        val naslovi = arrayOf("подешено", "тачно", "тачност", "стварно", "враћања", "")
        naslovi.forEachIndexed { i, t ->
            if (t.isNotEmpty()) c.drawText(t, MARGINA + kolone[i], y, zaglavljeP)
        }
        y += 6f
        linija(c, y)
        y += 14f

        r.stages.forEach { s ->
            val procenat = (s.accuracy * 100).toInt()
            val bojaRed = if (s.accuracy >= 0.80f) CRNA else CRVENA
            val p = paint(8.5f, bojaRed)

            c.drawText("${s.nominalWpm} WPM", MARGINA + kolone[0], y, p)
            c.drawText("${s.correct}/${s.total}", MARGINA + kolone[1], y, p)
            c.drawText("$procenat %", MARGINA + kolone[2], y, p)
            c.drawText("${s.effectiveWpm} WPM", MARGINA + kolone[3], y, celijaP)
            c.drawText("${s.backtracks}", MARGINA + kolone[4], y, celijaP)
            y += 15f
        }
        return y
    }
}
