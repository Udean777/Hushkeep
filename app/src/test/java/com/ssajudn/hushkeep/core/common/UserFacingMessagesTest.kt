package com.ssajudn.hushkeep.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UserFacingMessagesTest {
    @Test
    fun `remote exception details never reach user facing copy`() {
        val supabaseDetails = "new row violates row-level security policy URL: https://example.supabase.co/storage/v1/object"

        val message = UserFacingMessages.forError(
            AppError.Remote("Upload gagal", RuntimeException(supabaseDetails)),
        )

        assertEquals(UserFacingMessages.REMOTE_REQUEST_FAILED, message)
        assertFalse(message.contains("supabase", ignoreCase = true))
        assertFalse(message.contains("http", ignoreCase = true))
    }
}
