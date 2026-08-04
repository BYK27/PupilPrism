package com.example.pupilprism.ui.reader

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pupilprism.data.model.ReadingTelemetry
import kotlin.math.abs

/**
 * Приказ текуће речи са контекстом.
 *
 * Понашање зависи од тога да ли читање тече:
 *
 *   ТЕЧЕ       померање је искључено, текућа реч стоји тачно у средини, суседне
 *              речи тамне са удаљеношћу, а додир било где паузира. Премотавање
 *              додиром овде НЕМА смисла, јер се реч помера брже него што прст
 *              стигне до екрана.
 *
 *   ПАУЗИРАНО  померање је укључено и цео текст се може прегледати, све речи су
 *              читљиве, а додир на било коју премотава на њу.
 *
 * Сви редови имају ИСТУ висину. То није естетска одлука него услов да рачун
 * центрирања буде тачан: contentPadding се рачуна из висине реда, па би редови
 * различитих висина померали реч у фокусу.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ContextWords(
    words: List<String>,
    currentIndex: Int,
    isPaused: Boolean,
    isOrpEnabled: Boolean,
    isInteractive: Boolean,
    onWordClick: (Int) -> Unit,
    onTapWhilePlaying: () -> Unit,
    modifier: Modifier = Modifier,
    radius: Int = 3
) {
    val visinaReda = 64.dp
    val listState = rememberLazyListState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {

        val ivica = ((maxHeight - visinaReda) / 2).coerceAtLeast(0.dp)

        LaunchedEffect(currentIndex, isPaused) {
            if (!isPaused && currentIndex in words.indices) {
                listState.scrollToItem(currentIndex)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = ivica),
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = isPaused && isInteractive
        ) {
            items(words.size) { index ->
                val udaljenost = abs(index - currentIndex)
                val jeTekuca = index == currentIndex
                val rec = words[index].removeSuffix(ReadingTelemetry.OZNAKA_PASUSA)

                val prozirnost = when {
                    jeTekuca -> 1.0f
                    isPaused -> 0.55f
                    udaljenost == 1 -> 0.45f
                    udaljenost == 2 -> 0.28f
                    udaljenost <= radius -> 0.16f
                    else -> 0f
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(visinaReda)
                        .then(
                            if (isPaused && isInteractive) Modifier.clickable {
                                onWordClick(index)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (prozirnost <= 0f) return@Box

                    if (jeTekuca && isOrpEnabled) {
                        OrpWord(
                            word = rec,
                            style = LocalTextStyle.current.copy(
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            orpColor = MaterialTheme.colorScheme.primary,
                            showGuides = false,
                            height = visinaReda,
                            anchorFraction = 0.5f
                        )
                    } else {
                        Text(
                            text = rec,
                            fontSize = if (jeTekuca) 40.sp else 20.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = if (jeTekuca) FontWeight.Medium
                            else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.alpha(prozirnost)
                        )
                    }
                }
            }
        }

        if (!isPaused) {
            val interakcija = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = interakcija,
                        indication = null
                    ) { onTapWhilePlaying() }
            )
        }
    }
}
