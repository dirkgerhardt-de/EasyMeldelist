package de.dirkgerhardt.easymeldelist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.dirkgerhardt.easymeldelist.data.MeldeEntry
import de.dirkgerhardt.easymeldelist.data.SavedResultsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedDetailScreen(
    navController: NavController,
    fileName: String,
    participant: String,
    verein: String?
) {
    val context = LocalContext.current
    val repo = remember { SavedResultsRepository(context) }
    val entries: List<MeldeEntry> = remember {
        repo.loadEntries(fileName, participant, verein)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Wettkampfliste") },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
            }
        }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    participant + (verein?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(entries) { e ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Wettkampf ${e.wettkampf} · ${e.distanz} ${e.schwimmartAusgeschrieben}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Lauf ${e.lauf} · Bahn ${e.bahn}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        e.zeit?.let { zeit ->                                      // <-- ergänzen
                            Text(
                                "Meldezeit: $zeit",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Erweiterungsfunktion für die ausgeschriebene Schwimmart
private val MeldeEntry.schwimmartAusgeschrieben: String
    get() = when (schwimmart) {
        "F" -> "Freistil"
        "B" -> "Brust"
        "R" -> "Rücken"
        "S" -> "Schmetterling"
        "L" -> "Lagen"
        else -> schwimmart
    }