package com.ssajudn.hushkeep.core.media

import android.content.ContentResolver
import android.net.Uri
import com.ssajudn.hushkeep.core.common.AppError
import com.ssajudn.hushkeep.core.common.AppResult
import com.ssajudn.hushkeep.core.common.UriResolver
import com.ssajudn.hushkeep.data.local.dao.MediaObjectDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportManager(
    private val mediaObjectDao: MediaObjectDao,
    private val contentResolver: ContentResolver,
) {
    private val uriResolver = UriResolver(contentResolver)

    suspend fun exportAll(ownerId: String, destination: Uri): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val mediaObjects = mediaObjectDao.findAll(ownerId)
                contentResolver.openOutputStream(destination)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zip ->
                        val metadata = JSONArray()
                        mediaObjects.forEach { media ->
                            metadata.put(
                                JSONObject()
                                    .put("mediaId", media.id)
                                    .put("memoryId", media.memoryId)
                                    .put("fileName", media.fileName)
                                    .put("mimeType", media.mimeType)
                                    .put("sizeBytes", media.sizeBytes),
                            )

                            val localUri = media.localUri?.let(Uri::parse) ?: return@forEach
                            val inputStream = uriResolver.openInputStream(localUri)
                                ?: return@forEach
                            inputStream.use { input ->
                                zip.putNextEntry(
                                    ZipEntry(
                                        "photos/${
                                            safeFileName(
                                                media.fileName,
                                                media.id
                                            )
                                        }"
                                    )
                                )
                                input.copyTo(zip)
                                zip.closeEntry()
                            }
                        }
                        zip.putNextEntry(ZipEntry("metadata.json"))
                        zip.write(metadata.toString(2).toByteArray(Charsets.UTF_8))
                        zip.closeEntry()
                    }
                } ?: error("Tidak dapat menulis file export.")
            }.fold(
                onSuccess = { AppResult.Success(Unit) },
                onFailure = { error ->
                    AppResult.Failure(
                        AppError.Storage(
                            "Export gagal.",
                            error
                        )
                    )
                },
            )
        }

    private fun safeFileName(fileName: String, fallback: String): String {
        val cleanName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return cleanName.ifBlank { "$fallback.bin" }
    }
}
