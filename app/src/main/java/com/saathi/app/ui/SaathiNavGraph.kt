package com.saathi.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.saathi.app.SaathiApplication
import com.saathi.app.data.repository.SaathiRepository
import com.saathi.app.ui.archive.FamilyArchiveScreen
import com.saathi.app.ui.doctor.DoctorVisitCardScreen
import com.saathi.app.ui.document.DocumentScreen
import com.saathi.app.ui.helper.HelperWizardScreen
import com.saathi.app.ui.home.HomeScreen
import com.saathi.app.ui.medicine.MedicineScreen
import com.saathi.app.ui.navigator.LifeAdminNavigatorScreen
import com.saathi.app.ui.onboarding.OnboardingScreen
import com.saathi.app.ui.pharmacy.NearbyPharmacyScreen
import com.saathi.app.ui.privacy.PrivacyLedgerScreen
import com.saathi.app.ui.review.TenDayReviewScreen
import com.saathi.app.ui.scam.ScamAbhyasScreen
import com.saathi.app.ui.settings.SettingsScreen
import com.saathi.app.ui.sos.EmergencyCardScreen
import com.saathi.app.ui.splash.StartupWindowAnimation
import com.saathi.app.ui.verification.TrustVerificationScreen
import com.saathi.app.ui.voice.VoiceGuideScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Medicine : Screen("medicine")
    object Document : Screen("document")
    object VoiceGuide : Screen("voice_guide")
    object DoctorVisitCard : Screen("doctor_visit_card")
    object EmergencyCard : Screen("emergency_card")
    object PrivacyLedger : Screen("privacy_ledger")
    object NearbyPharmacy : Screen("nearby_pharmacy")
    object ScamAbhyas : Screen("scam_abhyas")
    object HelperWizard : Screen("helper_wizard")
    object LifeAdmin : Screen("life_admin")
    object TrustVerification : Screen("trust_verification")
    object FamilyArchive : Screen("family_archive")
    object TenDayReview : Screen("ten_day_review")
    object QrScanner : Screen("qr_scanner")
    object Settings : Screen("settings")
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

    val prefs = remember { context.getSharedPreferences("sevacare_prefs", Context.MODE_PRIVATE) }
    val hasCompletedOnboarding = remember { prefs.getBoolean("has_completed_onboarding", false) }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            StartupWindowAnimation(
                onAnimationFinished = {
                    val destination = if (hasCompletedOnboarding) Screen.Home.route else Screen.Onboarding.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateMedicine = { navController.navigate(Screen.Medicine.route) },
                onNavigateDocument = { navController.navigate(Screen.Document.route) },
                onNavigateVoiceGuide = { navController.navigate(Screen.VoiceGuide.route) },
                onNavigateLifeAdmin = { navController.navigate(Screen.LifeAdmin.route) },
                onNavigateTrustVerification = { navController.navigate(Screen.TrustVerification.route) },
                onNavigateFamilyArchive = { navController.navigate(Screen.FamilyArchive.route) },
                onNavigateTenDayReview = { navController.navigate(Screen.TenDayReview.route) },
                onTriggerSos = { navController.navigate(Screen.EmergencyCard.route) }
            )
        }
        composable(Screen.Medicine.route) {
            com.saathi.app.ui.medicine.MedicineHubScreen(
                onBack = { navController.popBackStack() },
                onNavigatePairing = { navController.navigate(Screen.QrScanner.route) }
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
        composable(Screen.DoctorVisitCard.route) {
            DoctorVisitCardScreen(
                medicines = emptyList(),
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.EmergencyCard.route) {
            EmergencyCardScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.PrivacyLedger.route) {
            PrivacyLedgerScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.NearbyPharmacy.route) {
            NearbyPharmacyScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ScamAbhyas.route) {
            ScamAbhyasScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.HelperWizard.route) {
            HelperWizardScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.LifeAdmin.route) {
            LifeAdminNavigatorScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.TrustVerification.route) {
            TrustVerificationScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.FamilyArchive.route) {
            FamilyArchiveScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.TenDayReview.route) {
            TenDayReviewScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.QrScanner.route) {
            val pairingRepo = remember {
                com.saathi.app.data.pairing.PairingRepository(app.database.familyPairDao())
            }
            com.saathi.app.ui.pairing.QrScannerScreen(
                pairingRepository = pairingRepo,
                onPairingSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
