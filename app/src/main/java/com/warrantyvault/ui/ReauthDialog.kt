package com.warrantyvault.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ReauthDialog(
    onDismiss: () -> Unit,
    onReauth: () -> Unit,
    onSignOut: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Session expired") },
        text = { 
            Text("For your security, please re-authenticate to continue. Your session has expired due to inactivity.")
        },
        confirmButton = {
            Button(onClick = onReauth) {
                Text("Re-authenticate")
            }
        },
        dismissButton = {
            TextButton(onClick = onSignOut) {
                Text("Sign out")
            }
        }
    )
}