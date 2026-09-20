package com.ssajudn.hushkeep.core.config

/**
 * Product-scope switches. MVP capabilities are enabled by default; future
 * roadmap capabilities stay disabled until their backend and privacy model are
 * implemented.
 */
object FeatureFlags {
    const val cloudBackup = true
    const val photoImport = true
    const val favorites = true
    const val trash = true
    const val otpEnabled = false

    const val video = false
    const val sharedAlbums = false
    const val linkSharing = false
    const val ai = false
    const val endToEndEncryption = false
}
