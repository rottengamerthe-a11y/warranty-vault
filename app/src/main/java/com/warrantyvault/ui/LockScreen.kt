package com.warrantyvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    onBiometricUnlock: () -> Unit = {},
    verifyPassword: (String) -> Boolean = { true },
    biometricAvailable: Boolean = false
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(VaultNight, VaultNightRaised, VaultNight))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Locked",
                tint = VaultMint,
                modifier = Modifier.size(64.dp)
            )
            Text(
                "Vault Locked",
                color = VaultText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Enter your password to unlock",
                color = VaultTextMuted,
                style = MaterialTheme.typography.bodyLarge
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = null
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VaultText,
                    unfocusedTextColor = VaultText,
                    focusedLabelColor = VaultMint,
                    unfocusedLabelColor = VaultTextMuted,
                    focusedBorderColor = VaultMint,
                    unfocusedBorderColor = VaultGlassBorder,
                    cursorColor = VaultMint
                )
            )
            error?.let {
                Text(it, color = VaultCoral, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = {
                    if (verifyPassword(password)) {
                        onUnlock()
                    } else {
                        error = "Incorrect password. Please try again."
                        password = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VaultMint),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Unlock", color = VaultNight, fontWeight = FontWeight.Bold)
            }
            if (biometricAvailable) {
                OutlinedButton(
                    onClick = onBiometricUnlock,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = VaultMint)
                    Spacer(Modifier.width(8.dp))
                    Text("Use biometrics", color = VaultMint)
                }
            }
        }
    }
}