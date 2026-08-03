package com.warrantyvault.security

import org.junit.Test
import kotlin.test.assertEquals

class SecuritySettingsTest {
    @Test
    fun autoLockTimeoutUsesDefaultWhenUnset() {
        assertEquals(5L * 60L * 1000L, SecuritySettings.getAutoLockTimeoutMs(0))
    }

    @Test
    fun autoLockTimeoutConvertsMinutesToMillis() {
        assertEquals(15L * 60L * 1000L, SecuritySettings.getAutoLockTimeoutMs(15))
    }
}
