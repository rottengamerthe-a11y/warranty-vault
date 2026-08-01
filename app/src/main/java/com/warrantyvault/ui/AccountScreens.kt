package com.warrantyvault.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

data class AccountCredentials(
    val name: String,
    val email: String,
    val password: String
)

@Composable
fun LaunchScreen() {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.86f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "launchScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "launchAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(VaultNight, VaultNightRaised, VaultNight))),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(VaultMint.copy(alpha = 0.24f), androidx.compose.ui.graphics.Color.Transparent),
                        center = Offset(420f, 520f),
                        radius = 680f
                    )
                )
        )
        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(VaultGlassStrong)
                    .border(1.dp, VaultGlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = VaultMint, modifier = Modifier.size(44.dp))
            }
            Text("WarrantyVault", color = VaultText, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Receipts, reminders, and peace of mind", color = VaultTextMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AuthScreen(
    savedEmail: String?,
    savedPassword: String?,
    onAuthenticated: (AccountCredentials) -> Unit
) {
    var signupMode by remember { mutableStateOf(savedEmail.isNullOrBlank()) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(savedEmail.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(VaultNight, VaultNightRaised, VaultNight)))
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(VaultGlassStrong)
                .border(1.dp, VaultGlassBorder, RoundedCornerShape(30.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                if (signupMode) Icons.Default.VerifiedUser else Icons.Default.Lock,
                contentDescription = null,
                tint = VaultMint,
                modifier = Modifier.size(38.dp)
            )
            Text(
                if (signupMode) "Create your vault" else "Welcome back",
                color = VaultText,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                if (signupMode) "Set up a local profile for this device." else "Sign in to continue to your saved vault.",
                color = VaultTextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            if (signupMode) {
                AuthField(value = name, label = "Name", onValueChange = { name = it })
            }
            AuthField(value = email, label = "Email", onValueChange = { email = it })
            AuthField(
                value = password,
                label = "Password",
                onValueChange = { password = it },
                password = true
            )
            error?.let { Text(it, color = VaultCoral, style = MaterialTheme.typography.bodyMedium) }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = {
                    error = null
                    val cleanEmail = email.trim()
                    when {
                        cleanEmail.isBlank() || password.length < 4 -> error = "Enter an email and a password with at least 4 characters."
                        signupMode -> onAuthenticated(AccountCredentials(name.trim().ifBlank { "Vault owner" }, cleanEmail, password))
                        savedEmail == null || savedPassword == null -> error = "Create an account first."
                        cleanEmail != savedEmail || password != savedPassword -> error = "Email or password does not match."
                        else -> onAuthenticated(AccountCredentials(name.trim().ifBlank { "Vault owner" }, cleanEmail, password))
                    }
                },
                contentPadding = PaddingValues(12.dp)
            ) {
                Text(if (signupMode) "Sign up" else "Log in")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (signupMode) "Already have a local account?" else "New here?",
                    color = VaultTextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = {
                    error = null
                    signupMode = !signupMode
                }) {
                    Text(if (signupMode) "Log in" else "Sign up")
                }
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    password: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
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
}
