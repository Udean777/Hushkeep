package com.ssajudn.hushkeep.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpPolicyTest {
    @Test
    fun `normalizes otp to digits and six characters`() {
        assertEquals("123456", OtpPolicy.normalize("12a3 4567"))
    }

    @Test
    fun `accepts exactly six digits`() {
        assertTrue(OtpPolicy.isValid("123456"))
        assertFalse(OtpPolicy.isValid("12345"))
        assertFalse(OtpPolicy.isValid("1234567"))
        assertFalse(OtpPolicy.isValid("12345a"))
    }
}
