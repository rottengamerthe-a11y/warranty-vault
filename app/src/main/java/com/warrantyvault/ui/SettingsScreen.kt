package com.warrantyvault.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.Crossfade
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Close
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warrantyvault.backup.GoogleDriveBackupManager
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    accountName: String,
    accountEmail: String,
    viewModel: AppViewModel,
    settingsViewModel: SettingsViewModel,
    googleDriveViewModel: GoogleDriveViewModel,
    currentTheme: ThemeConfig,
    onThemeChange: (ThemeConfig) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onShowTutorial: () -> Unit,
    onShowPrivacy: () -> Unit = {},
    onScreenshotProtectionChange: (Boolean) -> Unit = {},
    onChangePassword: (String, String) -> Boolean = { _, _ -> false }
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    val driveState by googleDriveViewModel.uiState.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var confirmClearData by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }
    var showBackupPasswordDialog by remember { mutableStateOf(false) }
    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var showDriveUploadPasswordDialog by remember { mutableStateOf(false) }
    var showDriveRestorePasswordDialog by remember { mutableStateOf(false) }
    var driveUploadPassword by remember { mutableStateOf("") }
    var driveRestorePassword by remember { mutableStateOf("") }
    var pendingDriveRestoreId by remember { mutableStateOf<String?>(null) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }

    // Settings state stored in preferences
    var remindersEnabled by remember { mutableStateOf(prefs.getBoolean("reminders_enabled", true)) }
    var defaultReminderDays by remember { mutableStateOf(prefs.getInt("default_reminder_days", 14)) }
    var biometricEnabled by remember { mutableStateOf(prefs.getBoolean("biometric_enabled", false)) }
    var appLockEnabled by remember { mutableStateOf(prefs.getBoolean("app_lock_enabled", false)) }
    var autoLockMinutes by remember { mutableStateOf(prefs.getInt("auto_lock_minutes", 5)) }
    var screenshotProtectionEnabled by remember { mutableStateOf(prefs.getBoolean("screenshot_protection", true)) }
    var sessionTimeoutMinutes by remember { mutableStateOf(prefs.getInt("session_timeout_minutes", 30)) }
    var cloudSyncEnabled by remember { mutableStateOf(prefs.getBoolean("cloud_sync_enabled", false)) }
    var cloudBackupEnabled by remember { mutableStateOf(prefs.getBoolean("cloud_backup_enabled", false)) }
    // Password reset variables - commented out until Firebase is properly configured
    // var showPasswordResetDialog by remember { mutableStateOf(false) }
    // var resetEmail by remember { mutableStateOf(accountEmail) }

    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        if (it != null) settingsViewModel.exportJson(it, backupPassword.takeIf { it.isNotEmpty() })
    }
    val csvExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        if (it != null) settingsViewModel.exportCsv(it)
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) settingsViewModel.restoreJson(it, restorePassword.takeIf { it.isNotEmpty() })
    }
    
    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                googleDriveViewModel.checkSignInStatus()
            } catch (e: ApiException) {
                // Handle sign-in failure
            }
        }
    }
    
    fun launchBackupWithPassword() {
        showBackupPasswordDialog = true
    }
    
    fun launchRestoreWithPassword() {
        showRestorePasswordDialog = true
    }
    
    fun launchDriveUploadWithPassword() {
        showDriveUploadPasswordDialog = true
    }
    
    fun launchDriveRestoreWithPassword(backupId: String) {
        pendingDriveRestoreId = backupId
        showDriveRestorePasswordDialog = true
    }
    
    fun launchChangePassword() {
        currentPassword = ""
        newPassword = ""
        confirmNewPassword = ""
        passwordChangeError = null
        showChangePasswordDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(padding)
                .background(Brush.linearGradient(listOf(VaultNight, VaultNightRaised, VaultNight))),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- Account section ---
            item {
                SectionLabel("Account")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = VaultGlassStrong)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBubble(Icons.Default.Person)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(accountName, color = VaultText, style = MaterialTheme.typography.titleLarge)
                            Text(accountEmail, color = VaultTextMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                SettingsAction(
                    icon = Icons.Default.Storage,
                    title = "Clear local data",
                    subtitle = "Remove all stored data from this device",
                    onClick = { confirmClearData = true }
                )
            }
            item {
                SettingsDangerAction(
                    icon = Icons.Default.DeleteForever,
                    title = "Delete account",
                    subtitle = "Permanently delete your account and all data",
                    onClick = { confirmDeleteAccount = true }
                )
            }
            item {
                SettingsAction(
                    icon = Icons.Default.Password,
                    title = "Change password",
                    subtitle = "Update your local vault password",
                    onClick = { launchChangePassword() }
                )
            }

            // --- Reminders section ---
            item {
                SectionLabel("Reminders")
            }
            item {
                SettingsToggle(
                    icon = Icons.Default.NotificationsActive,
                    title = "Enable reminders",
                    subtitle = "Get notified before warranty and return deadlines",
                    checked = remindersEnabled,
                    onCheckedChange = {
                        remindersEnabled = it
                        prefs.edit().putBoolean("reminders_enabled", it).apply()
                        settingsViewModel.setRemindersEnabled(it)
                    }
                )
            }
            if (remindersEnabled) {
                item {
                    SettingsStepper(
                        icon = Icons.Default.AutoGraph,
                        title = "Default reminder",
                        subtitle = "Notify $defaultReminderDays days before each deadline",
                        value = defaultReminderDays,
                        onDecrement = {
                            if (defaultReminderDays > 1) {
                                defaultReminderDays--
                                prefs.edit().putInt("default_reminder_days", defaultReminderDays).apply()
                            }
                        },
                        onIncrement = {
                            if (defaultReminderDays < 90) {
                                defaultReminderDays++
                                prefs.edit().putInt("default_reminder_days", defaultReminderDays).apply()
                            }
                        }
                    )
                }
            }

            // --- Security ---
            item {
                SectionLabel("Security")
            }
            item {
                SettingsToggle(
                    icon = Icons.Default.Security,
                    title = "Biometric unlock",
                    subtitle = "Use fingerprint or face recognition to unlock",
                    checked = biometricEnabled,
                    onCheckedChange = {
                        biometricEnabled = it
                        prefs.edit().putBoolean("biometric_enabled", it).apply()
                    }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Default.Block,
                    title = "App lock",
                    subtitle = "Require authentication after inactivity",
                    checked = appLockEnabled,
                    onCheckedChange = {
                        appLockEnabled = it
                        prefs.edit().putBoolean("app_lock_enabled", it).apply()
                    }
                )
            }
            if (appLockEnabled) {
                item {
                    SettingsStepper(
                        icon = Icons.Default.AutoGraph,
                        title = "Auto-lock timeout",
                        subtitle = "Lock after $autoLockMinutes minutes of inactivity",
                        value = autoLockMinutes,
                        onDecrement = {
                            if (autoLockMinutes > 1) {
                                autoLockMinutes--
                                prefs.edit().putInt("auto_lock_minutes", autoLockMinutes).apply()
                            }
                        },
                        onIncrement = {
                            if (autoLockMinutes < 60) {
                                autoLockMinutes++
                                prefs.edit().putInt("auto_lock_minutes", autoLockMinutes).apply()
                            }
                        }
                    )
                }
            }
            item {
                SettingsToggle(
                    icon = Icons.Default.Screenshot,
                    title = "Screenshot protection",
                    subtitle = "Prevent screenshots on sensitive screens",
                    checked = screenshotProtectionEnabled,
                    onCheckedChange = {
                        screenshotProtectionEnabled = it
                        prefs.edit().putBoolean("screenshot_protection", it).apply()
                        onScreenshotProtectionChange(it)
                    }
                )
            }
            item {
                SettingsStepper(
                    icon = Icons.Default.AutoGraph,
                    title = "Session timeout",
                    subtitle = "Require re-auth after $sessionTimeoutMinutes minutes",
                    value = sessionTimeoutMinutes,
                    onDecrement = {
                        if (sessionTimeoutMinutes > 5) {
                            sessionTimeoutMinutes -= 5
                            prefs.edit().putInt("session_timeout_minutes", sessionTimeoutMinutes).apply()
                        }
                    },
                    onIncrement = {
                        if (sessionTimeoutMinutes < 120) {
                            sessionTimeoutMinutes += 5
                            prefs.edit().putInt("session_timeout_minutes", sessionTimeoutMinutes).apply()
                        }
                    }
                )
            }

            // --- Cloud ---
            item {
                SectionLabel("Cloud Services")
            }
            item {
                GoogleDriveSettingsCard(
                    driveState = driveState,
                    onSignInClick = {
                        val signInClient = googleDriveViewModel.getSignInClient()
                        googleSignInLauncher.launch(signInClient.signInIntent)
                    },
                    onSignOutClick = { googleDriveViewModel.signOut() },
                    onUploadBackup = { launchDriveUploadWithPassword() },
                    onListBackups = { googleDriveViewModel.listBackups() },
                    onDeleteBackup = googleDriveViewModel::deleteBackup,
                    onRestoreBackup = { launchDriveRestoreWithPassword(it) },
                    onDismissError = { googleDriveViewModel.resetToNormal() }
                )
            }

            // --- Appearance ---
            item {
                SectionLabel("Appearance")
            }
            ThemeConfig.entries.forEach { theme ->
                item {
                    ThemePickerCard(
                        theme = theme,
                        selected = currentTheme == theme,
                        onClick = { onThemeChange(theme) }
                    )
                }
            }

            // --- Vault Data ---
            item {
                SectionLabel("Vault & Data")
            }
            item {
                SettingsInfo(
                    icon = Icons.Default.Storage,
                    title = "Vault stats",
                    subtitle = "${state.items.size} items — ${state.items.sumOf { it.attachments.size }} saved files"
                )
            }
            item {
                SettingsAction(
                    icon = Icons.Default.Backup,
                    title = "Export CSV",
                    subtitle = "Spreadsheet of all warranty items",
                    onClick = { csvExport.launch("warranty-vault.csv") }
                )
            }
            item {
                SettingsAction(
                    icon = Icons.Default.Backup,
                    title = "Backup JSON",
                    subtitle = "Full vault backup with all fields",
                    onClick = { launchBackupWithPassword() }
                )
            }
            item {
                SettingsAction(
                    icon = Icons.Default.Backup,
                    title = "Restore JSON",
                    subtitle = "Replace vault with a backup file",
                    onClick = { launchRestoreWithPassword() }
                )
            }
            item {
                SettingsDangerAction(
                    icon = Icons.Default.DeleteForever,
                    title = "Delete all items",
                    subtitle = "Remove every warranty and attachment permanently",
                    onClick = { confirmDeleteAll = true }
                )
            }

            // --- Resources ---
            item {
                SectionLabel("Resources")
            }
            item {
                SettingsAction(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    title = "Show tutorial",
                    subtitle = "Revisit the 5-step onboarding guide",
                    onClick = onShowTutorial
                )
            }
            item {
                SettingsAction(
                    icon = Icons.Default.Info,
                    title = "Privacy & Security",
                    subtitle = "View data storage and security information",
                    onClick = { onShowPrivacy() }
                )
            }

            // --- Sign out ---
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { confirmSignOut = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultCoral)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Dialogs
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("Your local account stays on this device. You can log back in with the same email and password.") },
            confirmButton = {
                Button(onClick = {
                    confirmSignOut = false
                    onSignOut()
                }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") }
            }
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete everything?") },
            text = { Text("This permanently removes all items, receipts, and attachments. This cannot be undone. Consider exporting a backup first.") },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.clearAllItems()
                        confirmDeleteAll = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultCoral)
                ) { Text("Delete all") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") }
            }
        )
    }

    if (confirmClearData) {
        AlertDialog(
            onDismissRequest = { confirmClearData = false },
            title = { Text("Clear all local data?") },
            text = { Text("This will remove all your warranty items, receipts, and settings from this device. Your account credentials will be kept. Consider exporting a backup first.") },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.clearAllItems()
                        // Clear settings
                        prefs.edit().clear().apply()
                        confirmClearData = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultCoral)
                ) { Text("Clear data") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearData = false }) { Text("Cancel") }
            }
        )
    }

    if (confirmDeleteAccount) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAccount = false },
            title = { Text("Delete account permanently?") },
            text = { Text("This will permanently delete your account and all data including credentials, warranty items, and attachments. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.clearAllItems()
                        // Clear all data including account
                        prefs.edit().clear().apply()
                        onSignOut()
                        confirmDeleteAccount = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultCoral)
                ) { Text("Delete account") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAccount = false }) { Text("Cancel") }
            }
        )
    }

    // Backup password dialog
    if (showBackupPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showBackupPasswordDialog = false
                backupPassword = ""
            },
            title = { Text("Backup encryption") },
            text = {
                Column {
                    Text("Enter a password to encrypt your backup, or leave empty for unencrypted backup.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Password (optional)") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        jsonExport.launch("warranty-vault.json")
                        showBackupPasswordDialog = false
                    }
                ) { Text("Backup") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showBackupPasswordDialog = false
                    backupPassword = ""
                }) { Text("Cancel") }
            }
        )
    }

    // Restore password dialog
    if (showRestorePasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showRestorePasswordDialog = false
                restorePassword = ""
            },
            title = { Text("Restore backup") },
            text = {
                Column {
                    Text("Enter the password if the backup is encrypted, or leave empty for unencrypted backup.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text("Password (if encrypted)") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        restore.launch(arrayOf("application/json"))
                        showRestorePasswordDialog = false
                    }
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRestorePasswordDialog = false
                    restorePassword = ""
                }) { Text("Cancel") }
            }
        )
    }

    // Google Drive upload password dialog
    if (showDriveUploadPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDriveUploadPasswordDialog = false
                driveUploadPassword = ""
            },
            title = { Text("Drive backup encryption") },
            text = {
                Column {
                    Text("Enter a password to encrypt your Drive backup, or leave empty for unencrypted backup.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = driveUploadPassword,
                        onValueChange = { driveUploadPassword = it },
                        label = { Text("Password (optional)") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        googleDriveViewModel.uploadBackup(password = driveUploadPassword.takeIf { it.isNotEmpty() })
                        showDriveUploadPasswordDialog = false
                        driveUploadPassword = ""
                    }
                ) { Text("Upload") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDriveUploadPasswordDialog = false
                    driveUploadPassword = ""
                }) { Text("Cancel") }
            }
        )
    }

    // Google Drive restore password dialog
    if (showDriveRestorePasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDriveRestorePasswordDialog = false
                driveRestorePassword = ""
                pendingDriveRestoreId = null
            },
            title = { Text("Restore Drive backup") },
            text = {
                Column {
                    Text("Enter the password if the backup is encrypted, or leave empty for unencrypted backup.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = driveRestorePassword,
                        onValueChange = { driveRestorePassword = it },
                        label = { Text("Password (if encrypted)") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val backupId = pendingDriveRestoreId
                        if (backupId != null) {
                            googleDriveViewModel.restoreBackup(backupId, driveRestorePassword.takeIf { it.isNotEmpty() })
                        }
                        showDriveRestorePasswordDialog = false
                        driveRestorePassword = ""
                        pendingDriveRestoreId = null
                    }
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDriveRestorePasswordDialog = false
                    driveRestorePassword = ""
                    pendingDriveRestoreId = null
                }) { Text("Cancel") }
            }
        )
    }

    // Change password dialog
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showChangePasswordDialog = false
                passwordChangeError = null
            },
            title = { Text("Change password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your current password and choose a new one.")
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it; passwordChangeError = null },
                        label = { Text("Current password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; passwordChangeError = null },
                        label = { Text("New password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it; passwordChangeError = null },
                        label = { Text("Confirm new password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    passwordChangeError?.let {
                        Text(it, color = VaultCoral, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            newPassword.length < 4 -> passwordChangeError = "New password must be at least 4 characters."
                            newPassword != confirmNewPassword -> passwordChangeError = "New passwords do not match."
                            !onChangePassword(currentPassword, newPassword) -> passwordChangeError = "Current password is incorrect."
                            else -> {
                                showChangePasswordDialog = false
                                passwordChangeError = null
                            }
                        }
                    }
                ) { Text("Change password") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showChangePasswordDialog = false
                    passwordChangeError = null
                }) { Text("Cancel") }
            }
        )
    }
    
    // Password reset dialog - commented out until Firebase is properly configured
    /*
    if (showPasswordResetDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPasswordResetDialog = false
                resetEmail = accountEmail
            },
            title = { Text("Reset password") },
            text = {
                Column {
                    Text("Enter your email address to receive a password reset link.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.sendPasswordResetEmail(resetEmail)
                        showPasswordResetDialog = false
                    }
                ) { Text("Send reset email") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPasswordResetDialog = false
                    resetEmail = accountEmail
                }) { Text("Cancel") }
            }
        )
    }
    */
}

@Composable
private fun ThemePickerCard(
    theme: ThemeConfig,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = VaultGlassStrong)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme colour swatch
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.night)
                    .border(
                        width = 2.dp,
                        color = if (selected) theme.accent else VaultGlassBorder,
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(theme.accent)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(theme.label, color = VaultText, fontWeight = FontWeight.SemiBold)
                    if (selected) {
                        Text("• Active", color = theme.accent, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(theme.subtitle, color = VaultTextMuted, style = MaterialTheme.typography.bodyMedium)
            }
            // Selection indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) theme.accent else VaultGlass)
                    .border(
                        width = 2.dp,
                        color = if (selected) theme.accent else VaultGlassBorder,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF102219))
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = VaultMint,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = VaultGlassStrong)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = VaultMint, modifier = Modifier.size(26.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = VaultText, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = VaultTextMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = VaultMint,
                    checkedThumbColor = Color(0xFF102219),
                    uncheckedTrackColor = VaultGlass,
                    uncheckedThumbColor = VaultTextMuted
                )
            )
        }
    }
}

@Composable
private fun SettingsStepper(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = VaultGlassStrong)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = VaultMint, modifier = Modifier.size(26.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = VaultText, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = VaultTextMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDecrement, enabled = value > 1) { Text("–", color = VaultMint) }
                Text(
                    value.toString(),
                    color = VaultText,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(32.dp),
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onIncrement, enabled = value < 90) { Text("+", color = VaultMint) }
            }
        }
    }
}

@Composable
private fun SettingsInfo(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = VaultGlassStrong)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = VaultMint, modifier = Modifier.size(26.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = VaultText, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = VaultTextMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SettingsAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = VaultGlassStrong)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = VaultMint, modifier = Modifier.size(26.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = VaultText, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = VaultTextMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SettingsDangerAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = VaultGlassStrong)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = VaultCoral, modifier = Modifier.size(26.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = VaultCoral, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = VaultTextMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun IconBubble(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(VaultMint.copy(alpha = 0.18f))
            .border(1.dp, VaultMint.copy(alpha = 0.42f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = VaultMint)
    }
}

@Composable
private fun GoogleDriveSettingsCard(
    driveState: GoogleDriveUiState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onUploadBackup: () -> Unit,
    onListBackups: () -> Unit,
	onDeleteBackup: (String) -> Unit,
    onRestoreBackup: (String) -> Unit,
    onDismissError: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = VaultGlassStrong)
    ) {
        Crossfade(targetState = driveState, label = "DriveStateAnimation") { state ->
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (state) {
                    is GoogleDriveUiState.Loading, is GoogleDriveUiState.Uploading -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = VaultMint)
                        }
                    }
                    is GoogleDriveUiState.SignedOut -> {
                        GoogleDriveSignedOutView(onSignInClick = onSignInClick)
                    }
                    is GoogleDriveUiState.SignedIn -> {
                        GoogleDriveSignedInView(
                            state = state,
                            onSignOutClick = onSignOutClick,
                            onUploadBackup = onUploadBackup,
                            onListBackups = onListBackups,
							onDismissError = onDismissError
                        )
                    }
                    is GoogleDriveUiState.BackupList -> {
                        BackupListView(
							backups = state.backups,
							onDeleteClick = onDeleteBackup,
							onRestoreClick = onRestoreBackup
						)
                    }
                    is GoogleDriveUiState.Error -> {
                        StatusMessage(
                            message = state.message,
                            isError = true,
                            onDismiss = onDismissError
                        )
                    }
					is GoogleDriveUiState.Success -> {
                        StatusMessage(
                            message = state.message,
                            isError = false,
                            onDismiss = onDismissError
                        )
                    }
					is GoogleDriveUiState.BackupSuccess -> {
						StatusMessage(
							message = state.message,
							isError = false,
							onDismiss = onDismissError
						)
					}
                }
            }
        }
    }
}

@Composable
private fun GoogleDriveSignedInView(
    state: GoogleDriveUiState.SignedIn,
    onSignOutClick: () -> Unit,
    onUploadBackup: () -> Unit,
    onListBackups: () -> Unit,
	onDismissError: () -> Unit
) {
    val isLoading = state.isLoading
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Cloud,
            contentDescription = null,
            tint = VaultMint,
            modifier = Modifier.size(26.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Google Drive Backup",
                color = VaultText,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Connected as ${state.accountEmail}",
                color = VaultTextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    
    if (state.storageUsage != null) {
        Text(
            "Storage: ${state.storageUsage}",
            color = VaultTextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
    
    if (state.errorMessage != null) {
        StatusMessage(
            message = state.errorMessage,
            isError = true,
            onDismiss = onDismissError
        )
    } else if (state.successMessage != null) {
        StatusMessage(
            message = state.successMessage,
            isError = false,
            onDismiss = onDismissError
        )
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onUploadBackup,
            enabled = !isLoading,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = VaultMint,
                contentColor = Color(0xFF102219)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Upload", style = MaterialTheme.typography.labelMedium)
        }
        Button(
            onClick = onListBackups,
            enabled = !isLoading,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = VaultMint
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("View", style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButton(
            onClick = onSignOutClick,
            enabled = !isLoading,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = VaultCoral
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Sign Out", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun GoogleDriveSignedOutView(onSignInClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CloudOff,
            contentDescription = null,
            tint = VaultTextMuted,
            modifier = Modifier.size(26.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Google Drive Backup",
                color = VaultText,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Not connected",
                color = VaultTextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    Button(
        onClick = onSignInClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = VaultMint,
            contentColor = Color(0xFF102219)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Connect Google Drive", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun BackupListView(
	backups: List<com.warrantyvault.backup.GoogleDriveBackupManager.DriveBackupInfo>,
	onDeleteClick: (String) -> Unit,
	onRestoreClick: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(200.dp) // Constrain height to avoid overly large cards
    ) {
        items(backups.size) { index ->
			val backup = backups[index]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
				modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(backup.name, color = VaultText, fontWeight = FontWeight.SemiBold)
                    Text("Created: ${backup.created.formatBackupDate()}", color = VaultTextMuted, style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    IconButton(onClick = { onRestoreClick(backup.id) }) {
                        Icon(Icons.Default.Backup, contentDescription = "Restore backup", tint = VaultMint)
                    }
                    IconButton(onClick = { onDeleteClick(backup.id) }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete backup", tint = VaultCoral)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isError) VaultCoral.copy(alpha = 0.2f) else VaultMint.copy(
                    alpha = 0.2f
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = message,
            color = if (isError) VaultCoral else VaultMint,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Dismiss message",
                tint = if (isError) VaultCoral else VaultMint
            )
        }
    }
}
