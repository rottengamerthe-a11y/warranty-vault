package com.warrantyvault.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

class SessionManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val lastAuthTime = AtomicLong(System.currentTimeMillis())
    private val _sessionValid = MutableStateFlow(true)
    val sessionValid: StateFlow<Boolean> = _sessionValid.asStateFlow()
    
    companion object {
        private const val DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000L // 30 minutes in milliseconds
        private const val KEY_SESSION_TIMEOUT = "session_timeout_minutes"
    }
    
    fun getSessionTimeout(): Long {
        val minutes = prefs.getInt(KEY_SESSION_TIMEOUT, 30)
        return minutes * 60 * 1000L
    }
    
    fun setSessionTimeoutFromSettings(settingsPrefs: SharedPreferences) {
        val minutes = settingsPrefs.getInt("session_timeout_minutes", 30)
        prefs.edit().putInt(KEY_SESSION_TIMEOUT, minutes).apply()
    }
    
    fun onAuthenticated() {
        lastAuthTime.set(System.currentTimeMillis())
        _sessionValid.value = true
    }
    
    fun checkSessionValidity(): Boolean {
        val elapsed = System.currentTimeMillis() - lastAuthTime.get()
        val isValid = elapsed < getSessionTimeout()
        _sessionValid.value = isValid
        return isValid
    }
    
    fun invalidateSession() {
        _sessionValid.value = false
    }
    
    fun requireReauth(): Boolean {
        return !checkSessionValidity()
    }
    
    fun getSessionRemainingTime(): Long {
        val elapsed = System.currentTimeMillis() - lastAuthTime.get()
        val remaining = getSessionTimeout() - elapsed
        return maxOf(0, remaining)
    }
    
    fun getSessionRemainingMinutes(): Int {
        return (getSessionRemainingTime() / (60 * 1000L)).toInt()
    }
}