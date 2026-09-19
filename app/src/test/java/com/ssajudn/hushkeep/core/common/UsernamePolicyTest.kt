package com.ssajudn.hushkeep.core.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsernamePolicyTest {
    @Test
    fun acceptsNormalizedUsernameCharacters() {
        assertTrue(UsernamePolicy.isValid("  Rani.memory  "))
        assertTrue(UsernamePolicy.isValid("hush_keep-01"))
    }

    @Test
    fun rejectsInvalidUsernameCharactersAndLength() {
        assertFalse(UsernamePolicy.isValid("ab"))
        assertFalse(UsernamePolicy.isValid("my username"))
        assertFalse(UsernamePolicy.isValid("username!"))
    }
}
