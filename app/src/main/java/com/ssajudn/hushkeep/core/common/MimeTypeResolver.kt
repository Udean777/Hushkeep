package com.ssajudn.hushkeep.core.common

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import java.util.Locale

object MimeTypeResolver {
    fun resolve(contentResolver: ContentResolver, uri: Uri): String? {
        return contentResolver.getType(uri)
            ?: uri.lastPathSegment
                ?.substringAfterLast('.', missingDelimiterValue = "")
                ?.takeIf(String::isNotBlank)
                ?.lowercase(Locale.ROOT)
                ?.let(MimeTypeMap.getSingleton()::getMimeTypeFromExtension)
    }

    fun extensionForMimeType(mimeType: String?): String? {
        return mimeType
            ?.lowercase(Locale.ROOT)
            ?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
    }
}
