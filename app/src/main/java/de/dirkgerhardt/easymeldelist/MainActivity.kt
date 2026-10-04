package de.dirkgerhardt.easymeldelist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.dirkgerhardt.easymeldelist.ui.SavedParticipantsScreen
import de.dirkgerhardt.easymeldelist.ui.screens.HomeScreen
import de.dirkgerhardt.easymeldelist.ui.screens.LibraryScreen
import de.dirkgerhardt.easymeldelist.ui.screens.ResultsScreen
import de.dirkgerhardt.easymeldelist.ui.screens.SavedDetailScreen
import de.dirkgerhardt.easymeldelist.ui.screens.UploadWizardScreen

class MainActivity : ComponentActivity() {

    private val vm: MeldelistViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MeldelistNavGraph(vm) }
    }
}

@Composable
fun MeldelistNavGraph(vm: MeldelistViewModel) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("library") { LibraryScreen(navController) }
        composable("upload") { UploadWizardScreen(navController, vm) }
        composable("search/{fileName}") { backStackEntry ->
            UploadWizardScreen(
                navController,
                vm,
                prefillFile = backStackEntry.arguments?.getString("fileName") ?: ""
            )
        }
        composable("results/{fileName}/{nachname}/{vorname}/{verein}") { backStackEntry ->
            ResultsScreen(
                navController = navController,
                fileName = backStackEntry.arguments?.getString("fileName") ?: "",
                nachname = backStackEntry.arguments?.getString("nachname") ?: "",
                vorname = backStackEntry.arguments?.getString("vorname") ?: "",
                verein = backStackEntry.arguments?.getString("verein")?.takeIf { it.isNotBlank() }
            )
        }
        composable("saved/{fileName}") { backStackEntry ->
            SavedParticipantsScreen(
                navController = navController,
                fileName = backStackEntry.arguments?.getString("fileName") ?: ""
            )
        }
        composable("savedDetail/{fileName}/{name}/{verein}") { backStackEntry ->
            SavedDetailScreen(
                navController = navController,
                fileName = backStackEntry.arguments?.getString("fileName") ?: "",
                name = backStackEntry.arguments?.getString("name") ?: "",
                verein = backStackEntry.arguments?.getString("verein") ?: ""
            )
        }
    }
}