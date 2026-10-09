package de.dirkgerhardt.easymeldelist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.dirkgerhardt.easymeldelist.data.AnzeigeModus
import de.dirkgerhardt.easymeldelist.data.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val settings by SettingsRepository.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Anzeige der Chancen",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ModusOption(
                titel = "Medaillenchancen",
                beschreibung = "Nüchterne Einschätzung plus wahrscheinlichste Medaille",
                selected = settings.modus == AnzeigeModus.MEDAILLENCHANCEN,
                onClick = { repo.setModus(AnzeigeModus.MEDAILLENCHANCEN) }
            )

            ModusOption(
                titel = "Pro-Modus",
                beschreibung = "Exakte Prozentwerte für Gold, Silber, Bronze und Gesamt",
                selected = settings.modus == AnzeigeModus.PRO,
                onClick = { repo.setModus(AnzeigeModus.PRO) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Motivation bei geringer Gesamtchance (<50 %)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Motivation bei geringen Chancen",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Wenn keine realistische Medaillenchance besteht (<50 % Gesamt), " +
                                    "wird stattdessen ein Motivationstext angezeigt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.motivationAnzeigen,
                        onCheckedChange = { repo.setMotivationAnzeigen(it) }
                    )
                }
            }

            Text(
                "Perspektiv-Medaille",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Perspektive zeigen ab:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Slider(
                        value = settings.perspektiveSchwelle.toFloat(),
                        onValueChange = {
                            repo.setPerspektiveSchwelle(it.toDouble())
                        },
                        valueRange = 0f..0.5f,
                        steps = 9,  // 5 %-Intervalle: 0 %, 5 %, 10 %, ..., 50 %
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "Mindest-Chance der BESSEREN Medaille, ab der sie als " +
                                "Perspektive angezeigt wird.\n\n" +
                                "Empfohlen: 20 % (empirische Schwelle)\n" +
                                "Niedriger = öfter Perspektive (mehr Aufstiegs-Motivation)\n" +
                                "Höher = konservativer (nur deutliche Aufstiegsmöglichkeiten)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ModusOption(
    titel: String,
    beschreibung: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(titel, style = MaterialTheme.typography.titleMedium)
                Text(
                    beschreibung,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}