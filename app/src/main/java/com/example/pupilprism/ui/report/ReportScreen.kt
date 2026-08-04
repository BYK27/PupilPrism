package com.example.pupilprism.ui.report

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.pupilprism.data.export.PdfReportGenerator
import com.example.pupilprism.data.model.ReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReportScreen(report: ReportData, onZavrsi: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ime by remember { mutableStateOf("") }
    var radiSe by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Процена је завршена", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ваша брзина уз очувано разумевање",
                    style = MaterialTheme.typography.labelLarge)
                Text("${report.optimalWpm} WPM",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Red("Подешено у просеку", "${report.prosecnaNominalna} WPM")
                Red("Стварно прочитано", "${report.prosecnaEfektivna} WPM")
                Red("Разлика", "${report.gubitakProcenat} %")
                Divider(Modifier.padding(vertical = 8.dp))
                Red("Прочитано речи", "${report.ukupnoReci}")
                Red("Време читања",
                    "${"%.1f".format(report.aktivnoVremeMs / 60000.0)} min")
                Red("Враћања уназад", "${report.ukupnoVracanja}")
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = ime,
            onValueChange = { ime = it },
            label = { Text("Име и презиме (необавезно)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Име се уписује само у PDF који добијате. Не чува се у апликацији " +
                    "нити у подацима истраживања.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                radiSe = true
                scope.launch {
                    val file = withContext(Dispatchers.IO) {
                        PdfReportGenerator.generate(context, report, ime)
                    }
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(intent, "Извештај о читању")
                    )
                    radiSe = false
                }
            },
            enabled = !radiSe,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (radiSe) "Припремам..." else "Преузми као PDF")
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onZavrsi, modifier = Modifier.fillMaxWidth()) {
            Text("Затвори")
        }
    }
}

@Composable
private fun Red(naziv: String, vrednost: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(naziv, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(vrednost, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium)
    }
}
