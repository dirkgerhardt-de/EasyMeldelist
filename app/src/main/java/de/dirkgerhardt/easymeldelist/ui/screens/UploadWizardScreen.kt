package de.dirkgerhardt.easymeldelist.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.dirkgerhardt.easymeldelist.MeldelistViewModel
import de.dirkgerhardt.easymeldelist.data.AnalysisCache
import de.dirkgerhardt.easymeldelist.data.MeldelistAnalysis
import de.dirkgerhardt.easymeldelist.data.MeldelistParser
import de.dirkgerhardt.easymeldelist.data.MeldelistRepository
import de.dirkgerhardt.easymeldelist.util.enc
import de.dirkgerhardt.easymeldelist.util.urlEncode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadWizardScreen(
    navController: NavController,
    vm: MeldelistViewModel,
    prefillFile: String? = null
) {
    val context = LocalContext.current
    val repository = remember { MeldelistRepository(context) }
    val parser = remember { MeldelistParser() }
    val cache = remember { AnalysisCache(context) }
    val scope = rememberCoroutineScope()

    var selectedClubExpanded by remember { mutableStateOf(false) }

    var loadStep by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val isLoading = loadStep != null

    val clubParticipants = remember(vm.participants, vm.selectedClub) {
        if (vm.selectedClub != null)
            vm.participants.filter { it.verein == vm.selectedClub }
        else
            vm.participants
    }

    val nachnameSuggestions = clubParticipants.map { it.nachname }.distinct()
        .filter {
            vm.nachname.isNotBlank() &&
                    it.startsWith(vm.nachname, ignoreCase = true) &&
                    !it.equals(vm.nachname, ignoreCase = true)
        }
        .take(8)

    val vornameSuggestions = if (clubParticipants.none {
            it.nachname.equals(vm.nachname, ignoreCase = true)
        }) emptyList()
    else {
        val base = clubParticipants
            .filter { it.nachname.equals(vm.nachname, ignoreCase = true) }
            .map { it.vorname }.distinct()

        if (vm.vorname.isBlank())
            base.take(8)
        else
            base.filter {
                it.startsWith(vm.vorname, ignoreCase = true) &&
                        !it.equals(vm.vorname, ignoreCase = true)
            }.take(8)
    }

    LaunchedEffect(vm.nachname) {
        val known = clubParticipants.any {
            it.nachname.equals(vm.nachname, ignoreCase = true)
        }
        if (!known && vm.vorname.isNotBlank()) {
            vm.vorname = ""
        }
    }

    fun loadExisting(fileName: String) {
        scope.launch {
            try {
                val cached = cache.load(fileName)

                if (cached != null) {
                    loadStep = "Öffne gespeicherte Daten…"
                    delay(300)
                    applyAnalysis(vm, fileName, cached)
                } else {
                    loadStep = "Analysiere Meldeliste…"
                    val analysis = parser.analyze(repository.open(fileName))
                    cache.save(fileName, analysis)
                    repository.saveEventName(fileName, analysis.eventName)
                    applyAnalysis(vm, fileName, analysis)
                }
            } catch (e: Exception) {
                error = "Fehler beim Lesen: ${e.message}"
            } finally {
                loadStep = null
            }
        }
    }

    LaunchedEffect(prefillFile) {
        val file = prefillFile ?: return@LaunchedEffect
        val stale = vm.importedFile != file || vm.participants.isEmpty()
        if (stale) loadExisting(file)
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    loadStep = "Lade Datei…"
                    val fileName = repository.import(uri)
                    cache.invalidate(fileName)
                    loadExisting(fileName)
                } catch (e: Exception) {
                    error = "Fehler beim Lesen: ${e.message}"
                    loadStep = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neue Meldeliste") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Schritt 1: Meldeliste auswählen",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(if (vm.importedFile == null) "PDF auswählen…" else "✓ ${vm.eventName ?: vm.importedFile}")
                }

                HorizontalDivider()

                Text(
                    "Schritt 2: Schwimmer suchen",
                    style = MaterialTheme.typography.titleMedium
                )

                if (vm.clubs.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = selectedClubExpanded,
                        onExpandedChange = { if (!isLoading) selectedClubExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = vm.selectedClub ?: "",
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isLoading,
                            label = { Text("Verein") },
                            placeholder = { Text("(optional – alle Vereine)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = selectedClubExpanded,
                            onDismissRequest = { selectedClubExpanded = false }
                        ) {
                            vm.clubs.forEach { club ->
                                DropdownMenuItem(
                                    text = { Text(club) },
                                    onClick = {
                                        vm.selectedClub = club
                                        selectedClubExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = nachnameSuggestions.isNotEmpty(),
                    onExpandedChange = { }
                ) {
                    OutlinedTextField(
                        value = vm.nachname,
                        onValueChange = { vm.nachname = it },
                        enabled = !isLoading,
                        label = { Text("Nachname") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = nachnameSuggestions.isNotEmpty(),
                        onDismissRequest = { }
                    ) {
                        nachnameSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = { vm.nachname = suggestion }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = vornameSuggestions.isNotEmpty(),
                    onExpandedChange = { }
                ) {
                    OutlinedTextField(
                        value = vm.vorname,
                        onValueChange = { vm.vorname = it },
                        enabled = !isLoading,
                        label = { Text("Vorname") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = vornameSuggestions.isNotEmpty(),
                        onDismissRequest = { }
                    ) {
                        vornameSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = { vm.vorname = suggestion }
                            )
                        }
                    }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Button(
                    onClick = {
                        navController.navigate(
                            "results/${urlEncode(vm.importedFile ?: "")}/${urlEncode(vm.nachname)}/${urlEncode(vm.vorname)}/${urlEncode(vm.selectedClub ?: "")}"
                        )
                    },
                    enabled = vm.importedFile != null && vm.nachname.isNotBlank()
                            && vm.vorname.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Suchen") }
            }

            if (loadStep != null) {
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
                                loadStep ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Bitte warten…",
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

private fun applyAnalysis(vm: MeldelistViewModel, fileName: String, analysis: MeldelistAnalysis) {
    vm.importedFile = fileName
    vm.eventName = analysis.eventName
    vm.clubs = analysis.clubs
    vm.participants = analysis.participants
    vm.selectedClub = null
    vm.nachname = ""
    vm.vorname = ""
}