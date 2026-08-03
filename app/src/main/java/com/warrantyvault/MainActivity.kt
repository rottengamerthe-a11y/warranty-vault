package com.warrantyvault

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.warrantyvault.security.BiometricAuthManager
import com.warrantyvault.security.InactivityTracker
import com.warrantyvault.security.PasswordHasher
import com.warrantyvault.security.SessionManager
import com.warrantyvault.ui.AccountCredentials
import com.warrantyvault.ui.AppViewModel
import com.warrantyvault.ui.AlertsScreen
import com.warrantyvault.ui.LockScreen
import com.warrantyvault.ui.PrivacyScreen
import com.warrantyvault.ui.ReauthDialog
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
import com.warrantyvault.ui.GoogleDriveViewModel
import com.warrantyvault.ui.ThemeConfig
import com.warrantyvault.ui.WarrantyVaultTheme
import kotlinx.coroutines.delay
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val googleDriveViewModel: GoogleDriveViewModel by viewModels()
    
    private val inactivityTracker = InactivityTracker(applicationContext)
    private val biometricAuthManager = BiometricAuthManager(applicationContext)
    private val sessionManager = SessionManager(applicationContext)

    override fun onResume() {
        super.onResume()
        inactivityTracker.checkInactivity()
        sessionManager.checkSessionValidity()
    }
    
    override fun onUserInteraction() {
        super.onUserInteraction()
        inactivityTracker.onUserActivity()
    }
    
    fun setScreenshotProtection(enabled: Boolean) {
        if (enabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

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
            var screenshotProtectionEnabled by remember { 
                mutableStateOf(settingsPrefs.getBoolean("screenshot_protection", true)) 
            }
            val isLocked by inactivityTracker.isLocked.collectAsState()
            val sessionValid by sessionManager.sessionValid.collectAsState()
            var showBiometricPrompt by remember { mutableStateOf(false) }
            var showReauthDialog by remember { mutableStateOf(false) }
            
            // Handle biometric authentication when locked
            LaunchedEffect(isLocked) {
                if (isLocked && settingsPrefs.getBoolean("biometric_enabled", false)) {
                    if (biometricAuthManager.canAuthenticate()) {
                        showBiometricPrompt = true
                    }
                }
            }
            
            LaunchedEffect(showBiometricPrompt) {
                if (showBiometricPrompt) {
                    try {
                        val success = biometricAuthManager.authenticate(this@MainActivity)
                        if (success) {
                            inactivityTracker.unlock()
                            sessionManager.onAuthenticated()
                        }
                    } catch (e: Exception) {
                        // Biometric failed or was cancelled — user can use password instead
                    }
                    showBiometricPrompt = false
                }
            }
            
            // Handle session expiration and re-authentication
            LaunchedEffect(sessionValid) {
                if (!sessionValid && signedIn) {
                    showReauthDialog = true
                }
            }
            
            // Handle screenshot protection setting
            LaunchedEffect(Unit) {
                setScreenshotProtection(settingsPrefs.getBoolean("screenshot_protection", true))
                sessionManager.setSessionTimeoutFromSettings(settingsPrefs)
            }
            
            // Apply screenshot protection when setting changes
            LaunchedEffect(screenshotProtectionEnabled) {
                setScreenshotProtection(screenshotProtectionEnabled)
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
                
                // Show lock screen if app is locked — requires real authentication
                if (isLocked) {
                    val storedPassword = accountPrefs.getString("password", null)
                    LockScreen(
                        onUnlock = {
                            inactivityTracker.unlock()
                            sessionManager.onAuthenticated()
                        },
                        onBiometricUnlock = {
                            if (settingsPrefs.getBoolean("biometric_enabled", false) && biometricAuthManager.canAuthenticate()) {
                                showBiometricPrompt = true
                            }
                        },
                        verifyPassword = { input ->
                            storedPassword != null && (
                                if (storedPassword.startsWith("pbkdf2$")) {
                                    PasswordHasher.verify(input, storedPassword)
                                } else {
                                    input == storedPassword
                                }
                            )
                        },
                        biometricAvailable = settingsPrefs.getBoolean("biometric_enabled", false) && biometricAuthManager.canAuthenticate()
                    )
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
                                .putString("password", PasswordHasher.hash(credentials.password))
                                .putBoolean("signed_in", true)
                                .apply()
                            accountName = credentials.name
                            accountEmail = credentials.email
                            signedIn = true
                            sessionManager.onAuthenticated()
                        }
                    )
                    return@WarrantyVaultTheme
                }
                
                // Show re-authentication dialog if session expired (overlay on top of content)
                if (showReauthDialog) {
                    ReauthDialog(
                        onDismiss = { showReauthDialog = false },
                        onReauth = {
                            showReauthDialog = false
                            // Trigger biometric or show auth screen
                            if (settingsPrefs.getBoolean("biometric_enabled", false) && biometricAuthManager.canAuthenticate()) {
                                showBiometricPrompt = true
                            } else {
                                // Navigate to auth screen
                                accountPrefs.edit().putBoolean("signed_in", false).apply()
                                signedIn = false
                            }
                        },
                        onSignOut = {
                            showReauthDialog = false
                            accountPrefs.edit().remove("password").putBoolean("signed_in", false).apply()
                            signedIn = false
                        }
                    )
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
                                googleDriveViewModel = googleDriveViewModel,
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
                                },
                                onShowPrivacy = {
                                    navController.navigate("privacy")
                                },
                                onScreenshotProtectionChange = { enabled ->
                                    screenshotProtectionEnabled = enabled
                                },
                                onChangePassword = { current, newPassword ->
                                    val stored = accountPrefs.getString("password", null)
                                    if (stored == null) return@SettingsScreen false
                                    val currentValid = if (stored.startsWith("pbkdf2$")) {
                                        PasswordHasher.verify(current, stored)
                                    } else {
                                        current == stored
                                    }
                                    if (currentValid) {
                                        accountPrefs.edit()
                                            .putString("password", PasswordHasher.hash(newPassword))
                                            .apply()
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )
                        }
                        composable("privacy") {
                            val state by viewModel.homeState.collectAsStateWithLifecycle()
                            PrivacyScreen(
                                contentPadding = contentPadding,
                                itemCount = state.items.size,
                                attachmentCount = state.items.sumOf { it.attachments.size },
                                onBack = { navController.popBackStack() }
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