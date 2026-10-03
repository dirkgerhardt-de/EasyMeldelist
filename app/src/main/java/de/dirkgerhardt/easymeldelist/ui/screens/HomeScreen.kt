package de.dirkgerhardt.easymeldelist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "EasyMeldelist",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Schwimm-Meldelisten",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate("library") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Meldelisten") }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("upload") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Neue Meldeliste hochladen") }
        }
    }
}