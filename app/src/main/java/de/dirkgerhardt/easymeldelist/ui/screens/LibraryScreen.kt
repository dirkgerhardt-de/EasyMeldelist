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
import de.dirkgerhardt.easymeldelist.util.enc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { MeldelistRepository(context) }
    var files by remember { repository.listFiles().let { mutableStateOf(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meldelisten") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        if (files.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Noch keine Meldelisten hochgeladen.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            navController.navigate("saved/${enc(file)}")
                        }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                repository.displayName(file),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }
    }
}