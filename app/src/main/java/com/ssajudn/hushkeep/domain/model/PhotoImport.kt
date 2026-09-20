package com.ssajudn.hushkeep.domain.model

import android.net.Uri

data class PhotoImport(
    val uri: Uri,
    val caption: String? = null,
)
