package com.ssajudn.hushkeep.core.common

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

data class UriMetadata(
    val displayName: String?,
    val sizeBytes: Long?,
    val mimeType: String?,
)

class UriResolver(private val contentResolver: ContentResolver) {
    fun inspect(uri: Uri): UriMetadata {
        return UriMetadata(
            displayName = queryDisplayName(uri),
            sizeBytes = querySize(uri),
            mimeType = MimeTypeResolver.resolve(contentResolver, uri),
        )
    }

    fun canRead(uri: Uri): Boolean {
        return runCatching {
            openInputStream(uri)?.use { input -> input.read() >= 0 } ?: false
        }.getOrElse { false }
    }

    fun openInputStream(uri: Uri): InputStream? {
        return if (uri.scheme == "file") {
            uri.path?.let(::File)?.takeIf(File::isFile)?.let(::FileInputStream)
        } else {
            contentResolver.openInputStream(uri)
        }
    }

    private fun queryDisplayName(uri: Uri): String? = query(uri) { cursor ->
        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            .takeIf { it >= 0 }
            ?.let(cursor::getString)
    }

    private fun querySize(uri: Uri): Long? = query(uri) { cursor ->
        cursor.getColumnIndex(OpenableColumns.SIZE)
            .takeIf { it >= 0 && !cursor.isNull(it) }
            ?.let(cursor::getLong)
    }

    private fun <T> query(uri: Uri, block: (Cursor) -> T): T? {
        return contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) block(cursor) else null
        }
    }
}
