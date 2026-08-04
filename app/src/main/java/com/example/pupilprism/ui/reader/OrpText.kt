package com.example.pupilprism.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun orpIndex(word: String): Int = when (word.length) {
    0, 1 -> 0
    in 2..5 -> 1
    in 6..9 -> 2
    in 10..13 -> 3
    else -> 4
}

@Composable
fun OrpWord(
    word: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    orpColor: Color = Color(0xFFD32F2F),
    textColor: Color = Color.Unspecified,
    showGuides: Boolean = true,
    guideColor: Color = Color.Gray,
    height: Dp = 96.dp,
    anchorFraction: Float = 0.42f
) {
    val measurer = rememberTextMeasurer()
    val boja = if (textColor == Color.Unspecified) style.color else textColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = word }
            .drawBehind {
                if (word.isEmpty()) return@drawBehind

                val i = orpIndex(word)
                val prefiks = word.substring(0, i)
                val orpZnak = word.substring(i, i + 1)
                val sufiks = word.substring(i + 1)

                val mPrefiks = measurer.measure(AnnotatedString(prefiks), style)
                val mOrp = measurer.measure(AnnotatedString(orpZnak), style)
                val mSufiks = measurer.measure(AnnotatedString(sufiks), style)

                val sidroX = size.width * anchorFraction
                val y = (size.height - mOrp.size.height) / 2f

                val pocetakX = sidroX - mPrefiks.size.width - mOrp.size.width / 2f

                if (showGuides) {
                    val duz = size.height * 0.16f
                    val debljina = 2f
                    drawLine(guideColor, Offset(sidroX, 0f), Offset(sidroX, duz), debljina)
                    drawLine(
                        guideColor,
                        Offset(sidroX, size.height - duz),
                        Offset(sidroX, size.height),
                        debljina
                    )
                }

                drawText(mPrefiks, color = boja, topLeft = Offset(pocetakX, y))
                drawText(
                    mOrp, color = orpColor,
                    topLeft = Offset(pocetakX + mPrefiks.size.width, y)
                )
                drawText(
                    mSufiks, color = boja,
                    topLeft = Offset(
                        pocetakX + mPrefiks.size.width + mOrp.size.width, y
                    )
                )
            }
    )
}
