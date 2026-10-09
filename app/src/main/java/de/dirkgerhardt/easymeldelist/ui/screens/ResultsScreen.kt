package de.dirkgerhardt.easymeldelist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.dirkgerhardt.easymeldelist.data.*
import de.dirkgerhardt.easymeldelist.util.urlEncode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    navController: NavController,
    fileName: String,
    nachname: String,
    vorname: String,
    verein: String?
) {
    val context = LocalContext.current
    val repository = remember { MeldelistRepository(context) }
    val parser = remember { MeldelistParser() }
    val savedRepo = remember { SavedResultsRepository(context) }

    var entries by remember { mutableStateOf<List<MeldeEntry>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    var wettkampfFeld by remember { mutableStateOf<Map<Int, List<RivalenZeit>>>(emptyMap()) }

    LaunchedEffect(fileName, nachname, vorname, verein) {
        android.util.Log.d("SEARCH_DEBUG", "fileName='$fileName' nachname='$nachname' vorname='$vorname' verein='$verein'")
        val feld = HashMap<Int, MutableList<RivalenZeit>>()
        entries = try {
            error = null
            parser.findSwimmer(
                repository.open(fileName), nachname, vorname, verein
            ) { wk, lauf, jahrgang, zeitMs, name ->
                val isOwn = name.contains("$nachname,", ignoreCase = true) &&
                        name.contains(vorname, ignoreCase = true)
                if (!isOwn) {
                    val list = feld[wk]
                    if (list == null) {
                        feld[wk] = mutableListOf(RivalenZeit(jahrgang, zeitMs))
                    } else {
                        list.add(RivalenZeit(jahrgang, zeitMs))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EasyMeldelist", "Suche fehlgeschlagen", e)
            error = e.message
            emptyList()
        }
        wettkampfFeld = feld
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Erfolg",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Speichern erfolgreich") },
            text = { Text("Die Ergebnisse wurden gespeichert.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        navController.navigate("saved/${urlEncode(fileName)}") {
                            popUpTo("home") { inclusive = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ergebnisse anzeigen") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDialog = false
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Neue Suche") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suchergebnis") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        bottomBar = {
            // Buttons immer sichtbar am unteren Rand – kein Scrollen nötig
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Abbrechen") }

                    Button(
                        onClick = {
                            entries?.let { result ->
                                savedRepo.save(
                                    fileName,
                                    "$nachname, $vorname",
                                    verein,
                                    result,
                                    wettkampfFeld
                                )
                                showDialog = true
                            }
                        },
                        enabled = entries?.isNotEmpty() == true,
                        modifier = Modifier.weight(1f)
                    ) { Text("Speichern") }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val result = entries) {
                null -> {}
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        val headline = when {
                            result.isEmpty() && error != null -> "Fehler: $error"
                            result.isEmpty() -> "Keine Meldungen gefunden."
                            else -> "$nachname, $vorname${verein?.let { " – $it" } ?: ""}"
                        }
                        Text(
                            headline,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = if (result.isEmpty() && error != null)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    result.groupBy { it.tag }.forEach { (tag, tagEntries) ->
                        if (tag > 0) {
                            item {
                                Text(
                                    "Tag $tag",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }

                        items(tagEntries) { entry ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Text(
                                        "Wettkampf ${entry.wettkampf} – ${entry.distanz}m ${entry.schwimmartAusgeschrieben}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Lauf ${entry.lauf} – Bahn ${entry.bahn}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (entries == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    ElevatedCard(modifier = Modifier.padding(32.dp)) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 6.dp,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                "Lade Daten für\n$nachname, $vorname",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private val MeldeEntry.schwimmartAusgeschrieben: String
    get() = when (schwimmart) {
        "F" -> "Freistil"
        "B" -> "Brust"
        "R" -> "Rücken"
        "S" -> "Schmetterling"
        "L" -> "Lagen"
        else -> schwimmart
    }