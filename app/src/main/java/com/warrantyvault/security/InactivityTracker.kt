package com.warrantyvault.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

class InactivityTracker(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val lastActivityTime = AtomicLong(System.currentTimeMillis())
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()
    
    fun onUserActivity() {
        // Only update the activity timestamp. Never auto-unlock — the user must
        // authenticate explicitly via biometrics or the lock screen.
        lastActivityTime.set(System.currentTimeMillis())
    }
    
    fun checkInactivity() {
        val appLockEnabled = prefs.getBoolean("app_lock_enabled", false)
        if (!appLockEnabled) return
        
        val autoLockMinutes = prefs.getInt("auto_lock_minutes", 5)
        val inactiveMillis = System.currentTimeMillis() - lastActivityTime.get()
        val lockThresholdMillis = autoLockMinutes * 60 * 1000L
        
        if (inactiveMillis > lockThresholdMillis && !_isLocked.value) {
            _isLocked.value = true
        }
    }
    
    fun lock() {
        _isLocked.value = true
    }
    
    fun unlock() {
        _isLocked.value = false
        lastActivityTime.set(System.currentTimeMillis())
    }
}