package com.ssajudn.hushkeep.core.config

object MediaConstraints {
    const val MAX_UPLOAD_BYTES: Long = 50L * 1024L * 1024L
    const val MAX_BATCH_ITEMS: Int = 50

    val supportedImageMimeTypes: Set<String> = setOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/heic",
        "image/heif",
    )
}
