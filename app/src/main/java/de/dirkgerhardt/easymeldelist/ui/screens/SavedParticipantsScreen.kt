package de.dirkgerhardt.easymeldelist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.dirkgerhardt.easymeldelist.data.MeldelistRepository
import de.dirkgerhardt.easymeldelist.data.SavedResultsRepository
import de.dirkgerhardt.easymeldelist.util.enc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedParticipantsScreen(navController: NavController, fileName: String) {
    val context = LocalContext.current
    val repo = remember { SavedResultsRepository(context) }
    val meldelistRepo = remember { MeldelistRepository(context) }
    val participants by remember { mutableStateOf(repo.loadParticipants(fileName)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(meldelistRepo.displayName(fileName)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { navController.navigate("search/${enc(fileName)}") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) { Text("Neue Suche") }
        }
    ) { padding ->
        if (participants.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Noch keine Teilnehmer gespeichert.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(participants) { p ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            navController.navigate(
                                "savedDetail/${enc(fileName)}/${enc(p.name)}/${enc(p.verein ?: "")}"
                            )
                        }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleSmall)
                            p.verein?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}