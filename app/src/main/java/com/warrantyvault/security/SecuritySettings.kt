package com.warrantyvault.security

/**
 * Central place for security-related settings helpers.
 * Kept as pure functions so they can be unit-tested without Android dependencies.
 */
object SecuritySettings {
    const val DEFAULT_AUTO_LOCK_MINUTES = 5
    const val DEFAULT_SESSION_TIMEOUT_MINUTES = 30
    const val MIN_AUTO_LOCK_MINUTES = 1
    const val MAX_AUTO_LOCK_MINUTES = 60
    const val MIN_SESSION_TIMEOUT_MINUTES = 5
    const val MAX_SESSION_TIMEOUT_MINUTES = 120

    /** Converts an auto-lock timeout in minutes to milliseconds, falling back to the default. */
    fun getAutoLockTimeoutMs(minutes: Int): Long {
        val effective = if (minutes > 0) minutes else DEFAULT_AUTO_LOCK_MINUTES
        return effective * 60L * 1000L
    }

    /** Converts a session timeout in minutes to milliseconds, falling back to the default. */
    fun getSessionTimeoutMs(minutes: Int): Long {
        val effective = if (minutes > 0) minutes else DEFAULT_SESSION_TIMEOUT_MINUTES
        return effective * 60L * 1000L
    }

    /** Clamps an auto-lock value into the supported range. */
    fun clampAutoLockMinutes(minutes: Int): Int = minutes.coerceIn(MIN_AUTO_LOCK_MINUTES, MAX_AUTO_LOCK_MINUTES)

    /** Clamps a session timeout value into the supported range. */
    fun clampSessionTimeoutMinutes(minutes: Int): Int = minutes.coerceIn(MIN_SESSION_TIMEOUT_MINUTES, MAX_SESSION_TIMEOUT_MINUTES)
}