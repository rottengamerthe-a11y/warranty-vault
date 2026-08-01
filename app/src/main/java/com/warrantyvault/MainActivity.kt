package com.warrantyvault

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.warrantyvault.ui.AccountCredentials
import com.warrantyvault.ui.AppViewModel
import com.warrantyvault.ui.AlertsScreen
import com.warrantyvault.ui.AuthScreen
import com.warrantyvault.ui.DetailScreen
import com.warrantyvault.ui.EditItemScreen
import com.warrantyvault.ui.HomeScreen
import com.warrantyvault.ui.HomeViewModel
import com.warrantyvault.ui.LaunchScreen
import com.warrantyvault.ui.OnboardingScreen
import com.warrantyvault.ui.ReceiptsScreen
import com.warrantyvault.ui.ScanReceiptScreen
import com.warrantyvault.ui.SettingsScreen
import com.warrantyvault.ui.SettingsViewModel
import com.warrantyvault.ui.ThemeConfig
import com.warrantyvault.ui.WarrantyVaultTheme
import kotlinx.coroutines.delay
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val accountPrefs = remember {
                com.warrantyvault.security.EncryptedPrefs.create(applicationContext, "account_prefs")
            }
            val settingsPrefs = remember { getSharedPreferences("settings", Context.MODE_PRIVATE) }
            var launchComplete by remember { mutableStateOf(false) }
            var accountName by remember {
                mutableStateOf(accountPrefs.getString("name", "Vault owner").orEmpty())
            }
            var accountEmail by remember { mutableStateOf(accountPrefs.getString("email", null)) }
            var signedIn by remember { mutableStateOf(accountPrefs.getBoolean("signed_in", false)) }
            var onboardingComplete by remember {
                mutableStateOf(settingsPrefs.getBoolean("onboarding_complete", false))
            }
            var showTutorial by remember { mutableStateOf(false) }
            var currentTheme by remember {
                mutableStateOf(ThemeConfig.fromKey(settingsPrefs.getString("theme", "vault") ?: "vault"))
            }

            // Wrap with the selected theme — all composables inside pick it up
            WarrantyVaultTheme(themeConfig = currentTheme) {
                LaunchedEffect(Unit) {
                    delay(1250)
                    launchComplete = true
                }

                if (!launchComplete) {
                    LaunchScreen()
                    return@WarrantyVaultTheme
                }

                // Onboarding for new sign-ups
                if (signedIn && !onboardingComplete && !showTutorial) {
                    OnboardingScreen(
                        onComplete = {
                            settingsPrefs.edit().putBoolean("onboarding_complete", true).apply()
                            onboardingComplete = true
                        }
                    )
                    return@WarrantyVaultTheme
                }

                // Manual tutorial replay
                if (showTutorial) {
                    OnboardingScreen(
                        onComplete = {
                            showTutorial = false
                        }
                    )
                    return@WarrantyVaultTheme
                }

                if (!signedIn) {
                    AuthScreen(
                        savedEmail = accountPrefs.getString("email", null),
                        savedPassword = accountPrefs.getString("password", null),
                        onAuthenticated = { credentials: AccountCredentials ->
                            accountPrefs.edit()
                                .putString("name", credentials.name)
                                .putString("email", credentials.email)
                                .putString("password", credentials.password)
                                .putBoolean("signed_in", true)
                                .apply()
                            accountName = credentials.name
                            accountEmail = credentials.email
                            signedIn = true
                        }
                    )
                    return@WarrantyVaultTheme
                }

                val snackbarHostState = remember { SnackbarHostState() }
                val notificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {}
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                LaunchedEffect(Unit) {
                    viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
                }
                LaunchedEffect(Unit) {
                    settingsViewModel.messages.collect { snackbarHostState.showSnackbar(it) }
                }

                val navController = rememberNavController()
                val initialItemId = intent.getLongExtra(EXTRA_ITEM_ID, 0L)
                LaunchedEffect(initialItemId) {
                    if (initialItemId > 0) navController.navigate("detail/$initialItemId")
                }

                Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { contentPadding ->
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                contentPadding = contentPadding,
                                viewModel = homeViewModel,
                                settingsViewModel = settingsViewModel,
                                onAdd = { navController.navigate("scan") },
                                onAlerts = { navController.navigate("alerts") },
                                onReceipts = { navController.navigate("receipts") },
                                onSettings = { navController.navigate("settings") },
                                onOpen = { navController.navigate("detail/$it") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                contentPadding = contentPadding,
                                accountName = accountName.ifBlank { "Vault owner" },
                                accountEmail = accountEmail.orEmpty(),
                                viewModel = viewModel,
                                settingsViewModel = settingsViewModel,
                                currentTheme = currentTheme,
                                onThemeChange = { theme ->
                                    currentTheme = theme
                                    settingsPrefs.edit().putString("theme", theme.key).apply()
                                },
                                onBack = { navController.popBackStack() },
                                onSignOut = {
                                    accountPrefs.edit().remove("password").putBoolean("signed_in", false).apply()
                                    signedIn = false
                                },
                                onShowTutorial = {
                                    navController.popBackStack()
                                    showTutorial = true
                                }
                            )
                        }
                        composable("alerts") {
                            AlertsScreen(
                                contentPadding = contentPadding,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onOpen = { navController.navigate("detail/$it") }
                            )
                        }
                        composable("receipts") {
                            ReceiptsScreen(
                                contentPadding = contentPadding,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onAdd = { navController.navigate("scan") },
                                onOpen = { navController.navigate("detail/$it") }
                            )
                        }
                        composable("scan") {
                            ScanReceiptScreen(
                                contentPadding = contentPadding,
                                onBack = { navController.popBackStack() },
                                onManualEntry = {
                                    navController.popBackStack()
                                    navController.navigate("edit/0")
                                },
                                onScanReady = { draft ->
                                    viewModel.stageScanDraft(draft)
                                    navController.popBackStack()
                                    navController.navigate("edit/0")
                                }
                            )
                        }
                        composable("detail/{itemId}") { entry ->
                            val itemId = entry.arguments?.getString("itemId")?.toLongOrNull() ?: 0L
                            DetailScreen(
                                contentPadding = contentPadding,
                                itemId = itemId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onEdit = { navController.navigate("edit/$itemId") }
                            )
                        }
                        composable("edit/{itemId}") { entry ->
                            val itemId = entry.arguments?.getString("itemId")?.toLongOrNull() ?: 0L
                            EditItemScreen(
                                contentPadding = contentPadding,
                                itemId = itemId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onSaved = { savedId ->
                                    navController.popBackStack()
                                    if (itemId == 0L) navController.navigate("detail/$savedId")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "itemId"
    }
}
