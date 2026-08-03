package com.warrantyvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemCount: Int,
    attachmentCount: Int,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(padding)
                .background(Brush.linearGradient(listOf(VaultNight, VaultNightRaised, VaultNight)))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Data Overview Section
            PrivacySection(title = "Data Overview") {
                PrivacyInfoItem(
                    icon = Icons.Default.Storage,
                    label = "Warranty Items",
                    value = "$itemCount items stored locally"
                )
                PrivacyInfoItem(
                    icon = Icons.Default.AttachFile,
                    label = "Attachments",
                    value = "$attachmentCount files stored locally"
                )
                PrivacyInfoItem(
                    icon = Icons.Default.Person,
                    label = "Account Data",
                    value = "Encrypted credentials stored locally"
                )
            }

            // Security Measures Section
            PrivacySection(title = "Security Measures") {
                PrivacyFeatureItem(
                    icon = Icons.Default.Security,
                    title = "AES-256 Encryption",
                    description = "All sensitive data is encrypted using military-grade AES-256 encryption"
                )
                PrivacyFeatureItem(
                    icon = Icons.Default.Lock,
                    title = "Android KeyStore",
                    description = "Encryption keys are stored in hardware-backed Android KeyStore"
                )
                PrivacyFeatureItem(
                    icon = Icons.Default.Password,
                    title = "Encrypted Preferences",
                    description = "Settings and preferences are stored using EncryptedSharedPreferences"
                )
                PrivacyFeatureItem(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Authentication",
                    description = "Optional biometric unlock using fingerprint or face recognition"
                )
                PrivacyFeatureItem(
                    icon = Icons.Default.ScreenLockPortrait,
                    title = "App Lock",
                    description = "Automatic app locking after configurable inactivity period"
                )
                PrivacyFeatureItem(
                    icon = Icons.Default.NoPhotography,
                    title = "Screenshot Protection",
                    description = "Prevents screenshots on sensitive screens when enabled"
                )
            }

            // Data Location Section
            PrivacySection(title = "Data Location") {
                PrivacyTextItem(
                    "All your data is stored locally on this device only. No data is transmitted to external servers or cloud services. You have full control over your warranty information and attachments."
                )
            }

            // Backup Security Section
            PrivacySection(title = "Backup Security") {
                PrivacyFeatureItem(
                    icon = Icons.Default.Password,
                    title = "Password Protection",
                    description = "Backups can be encrypted with a password using PBKDF2 key derivation with 100,000 iterations"
                )
                PrivacyFeatureItem(
                    icon = Icons.Default.Backup,
                    title = "Export Control",
                    description = "You choose whether to export encrypted or unencrypted backups"
                )
            }

            // Your Rights Section
            PrivacySection(title = "Your Rights") {
                PrivacyFeatureItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Data Deletion",
                    description = "You can clear all local data or permanently delete your account at any time"
                )
                PrivacyFeatureItem(
                    icon = Icons.Default.CloudDownload,
                    title = "Data Export",
                    description = "Export your data in JSON or CSV format anytime, with optional encryption"
                )
            }

            // Cloud Services Section
            PrivacySection(title = "Cloud Services (Optional)") {
                PrivacyFeatureItem(
                    icon = Icons.Default.Cloud,
                    title = "Encrypted Cloud Backup",
                    description = "Optional encrypted backups to cloud storage with password protection"
                )
                PrivacyFeatureItem(
                    icon = Icons.Default.CloudDone,
                    title = "Multi-Device Sync",
                    description = "Optional synchronization of warranty data across your devices"
                )
                PrivacyTextItem(
                    "Cloud services are completely optional. Your data works perfectly with local-only storage. When enabled, all cloud data is encrypted before upload."
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VaultGlassStrong)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = VaultMint,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Privacy First",
                            color = VaultText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        "Warranty Vault is designed with privacy as the foundation. Your data belongs to you, and we ensure it stays that way through local-only storage and strong encryption.",
                        color = VaultTextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            color = VaultMint,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VaultGlassStrong)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun PrivacyInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = VaultMint,
            modifier = Modifier.size(24.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                color = VaultText,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                value,
                color = VaultTextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PrivacyFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = VaultMint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                title,
                color = VaultText,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            description,
            color = VaultTextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 28.dp)
        )
    }
}

@Composable
private fun PrivacyTextItem(text: String) {
    Text(
        text,
        color = VaultTextMuted,
        style = MaterialTheme.typography.bodyMedium
    )
}