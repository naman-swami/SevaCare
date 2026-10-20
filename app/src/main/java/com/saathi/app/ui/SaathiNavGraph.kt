package com.saathi.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.saathi.app.SaathiApplication
import com.saathi.app.data.repository.SaathiRepository
import com.saathi.app.ui.document.DocumentScreen
import com.saathi.app.ui.home.HomeScreen
import com.saathi.app.ui.medicine.MedicineScreen
import com.saathi.app.ui.voice.VoiceGuideScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Medicine : Screen("medicine")
    object Document : Screen("document")
    object VoiceGuide : Screen("voice_guide")
}

@Composable
fun SaathiNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as SaathiApplication
    val repository = SaathiRepository(
        medicineDao = app.database.medicineDao(),
        scamPhraseDao = app.database.scamPhraseDao()
    )

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateMedicine = { navController.navigate(Screen.Medicine.route) },
                onNavigateDocument = { navController.navigate(Screen.Document.route) },
                onNavigateVoiceGuide = { navController.navigate(Screen.VoiceGuide.route) }
            )
        }
        composable(Screen.Medicine.route) {
            MedicineScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Document.route) {
            DocumentScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.VoiceGuide.route) {
            VoiceGuideScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
