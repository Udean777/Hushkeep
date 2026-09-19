package com.ssajudn.hushkeep.core.config

import org.junit.Assert.assertEquals
import org.junit.Test

class StoragePathsTest {
    @Test
    fun `builds a private original object path`() {
        assertEquals(
            "user-1/media-1/original.jpg",
            StoragePaths.originalObjectPath("user-1", "media-1", ".JPG"),
        )
    }
}
