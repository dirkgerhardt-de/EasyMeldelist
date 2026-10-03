package de.dirkgerhardt.easymeldelist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.dirkgerhardt.easymeldelist.ui.screens.HomeScreen
import de.dirkgerhardt.easymeldelist.ui.screens.LibraryScreen
import de.dirkgerhardt.easymeldelist.ui.screens.ResultsScreen
import de.dirkgerhardt.easymeldelist.ui.screens.SavedDetailScreen
import de.dirkgerhardt.easymeldelist.ui.screens.SavedParticipantsScreen
import de.dirkgerhardt.easymeldelist.ui.screens.UploadWizardScreen
import de.dirkgerhardt.easymeldelist.util.dec

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
        composable(
            route = "search/{fileName}"
        ) { backStackEntry ->
            UploadWizardScreen(
                navController,
                vm,
                prefillFile = dec(backStackEntry.arguments?.getString("fileName"))
            )
        }
        composable(
            route = "results/{fileName}/{nachname}/{vorname}/{verein}"
        ) { backStackEntry ->
            ResultsScreen(
                navController = navController,
                fileName = dec(backStackEntry.arguments?.getString("fileName")),
                nachname = dec(backStackEntry.arguments?.getString("nachname")),
                vorname = dec(backStackEntry.arguments?.getString("vorname")),
                verein = dec(backStackEntry.arguments?.getString("verein")).takeIf { it.isNotBlank() }
            )
        }
        composable(
            route = "saved/{fileName}"
        ) { backStackEntry ->
            SavedParticipantsScreen(
                navController,
                dec(backStackEntry.arguments?.getString("fileName"))
            )
        }
        composable(
            route = "savedDetail/{fileName}/{participant}/{verein}"
        ) { backStackEntry ->
            SavedDetailScreen(
                navController,
                dec(backStackEntry.arguments?.getString("fileName")),
                dec(backStackEntry.arguments?.getString("participant")),
                dec(backStackEntry.arguments?.getString("verein")).takeIf { it.isNotBlank() }
            )
        }
    }
}