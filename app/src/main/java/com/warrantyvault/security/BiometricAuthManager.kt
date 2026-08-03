package com.warrantyvault.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BiometricAuthManager(private val context: Context) {
    
    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }
    
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Warranty Vault",
        subtitle: String = "Confirm your identity to continue",
        description: String = "Use your fingerprint or device credential"
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        
        val biometricPrompt = BiometricPrompt(
            activity,
            context.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    continuation.resume(true)
                }
                
                override fun onAuthenticationFailed() {
                    // Do nothing - user can try again
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || 
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        continuation.resume(false)
                    } else {
                        continuation.resumeWithException(Exception("Biometric error: $errString"))
                    }
                }
            }
        )
        
        biometricPrompt.authenticate(promptInfo)
        
        continuation.invokeOnCancellation {
            // Cannot cancel biometric prompt once started
        }
    }
}