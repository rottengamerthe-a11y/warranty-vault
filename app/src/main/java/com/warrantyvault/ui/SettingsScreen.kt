package com.warrantyvault.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Storage
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    accountName: String,
    accountEmail: String,
    viewModel: AppViewModel,
    settingsViewModel: SettingsViewModel,
    currentTheme: ThemeConfig,
    onThemeChange: (ThemeConfig) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onShowTutorial: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    // Settings state stored in preferences
    var remindersEnabled by remember { mutableStateOf(prefs.getBoolean("reminders_enabled", true)) }
    var defaultReminderDays by remember { mutableStateOf(prefs.getInt("default_reminder_days", 14)) }

    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        if (it != null) settingsViewModel.exportJson(it)
    }
    val csvExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        if (it != null) settingsViewModel.exportCsv(it)
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) settingsViewModel.restoreJson(it)
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
                    onClick = { jsonExport.launch("warranty-vault.json") }
                )
            }
            item {
                SettingsAction(
                    icon = Icons.Default.Backup,
                    title = "Restore JSON",
                    subtitle = "Replace vault with a backup file",
                    onClick = { restore.launch(arrayOf("application/json")) }
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
                SettingsInfo(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy",
                    subtitle = "All vault data is stored locally on this device only"
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
