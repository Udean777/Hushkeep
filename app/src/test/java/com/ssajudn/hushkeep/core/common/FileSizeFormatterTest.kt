package com.ssajudn.hushkeep.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizeFormatterTest {
    @Test
    fun `formats bytes and binary units`() {
        assertEquals("0 B", FileSizeFormatter.format(0))
        assertEquals("1.0 KB", FileSizeFormatter.format(1024))
        assertEquals("1.0 MB", FileSizeFormatter.format(1024 * 1024))
    }
}
