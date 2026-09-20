package com.ssajudn.hushkeep.core.media

import android.content.ContentResolver
import android.net.Uri
import com.ssajudn.hushkeep.core.common.AppError
import com.ssajudn.hushkeep.core.common.AppResult
import com.ssajudn.hushkeep.core.common.UriResolver
import com.ssajudn.hushkeep.data.local.dao.AlbumDao
import com.ssajudn.hushkeep.data.local.dao.MediaObjectDao
import com.ssajudn.hushkeep.data.local.dao.MemoryDao
import com.ssajudn.hushkeep.data.local.entity.AlbumEntity
import com.ssajudn.hushkeep.data.local.entity.MediaObjectEntity
import com.ssajudn.hushkeep.data.local.entity.MemoryEntity
import com.ssajudn.hushkeep.data.remote.SupabaseMemoryDataSource
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportManager(
    private val mediaObjectDao: MediaObjectDao,
    private val memoryDao: MemoryDao,
    private val albumDao: AlbumDao,
    private val contentResolver: ContentResolver,
    supabaseClient: SupabaseClient?,
) {
    private val uriResolver = UriResolver(contentResolver)
    private val cloudSource = supabaseClient?.let(::SupabaseMemoryDataSource)

    suspend fun exportAll(ownerId: String, destination: Uri): AppResult<Unit> =
        export(ownerId, destination, memoryId = null, albumId = null)

    suspend fun exportMemory(ownerId: String, memoryId: String, destination: Uri): AppResult<Unit> =
        export(ownerId, destination, memoryId = memoryId, albumId = null)

    suspend fun downloadPhoto(ownerId: String, memoryId: String, destination: Uri): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val memory = memoryDao.findById(memoryId)
                    ?.takeIf { it.ownerId == ownerId && it.deletedAtEpochMs == null }
                    ?: error("Foto tidak ditemukan.")
                val media = mediaObjectDao.findForMemory(memory.id).firstOrNull()
                    ?: error("Media foto tidak ditemukan.")
                val signedUrl = media.storagePath?.let { path ->
                    cloudSource?.signedUrls(listOf(path))?.get(path)
                }
                val source = media.localUri?.let(Uri::parse)?.let(uriResolver::openInputStream)
                    ?: signedUrl?.let { URL(it).openStream() }
                    ?: error("Foto belum tersedia untuk diunduh.")
                val output = contentResolver.openOutputStream(destination)
                    ?: error("Tidak dapat menulis file download.")
                source.use { input -> output.use { input.copyTo(it) } }
            }.fold(
                onSuccess = { AppResult.Success(Unit) },
                onFailure = { error -> AppResult.Failure(AppError.Storage("Download gagal.", error)) },
            )
        }

    suspend fun exportAlbum(ownerId: String, albumId: String, destination: Uri): AppResult<Unit> =
        export(ownerId, destination, memoryId = null, albumId = albumId)

    private suspend fun export(
        ownerId: String,
        destination: Uri,
        memoryId: String?,
        albumId: String?,
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val allMemories = memoryDao.findAllForOwner(ownerId)
                .filter { it.deletedAtEpochMs == null }
            val selectedMemories = when {
                memoryId != null -> allMemories.filter { it.id == memoryId }
                albumId != null -> allMemories.filter { it.albumId == albumId }
                else -> allMemories
            }
            require(memoryId == null || selectedMemories.isNotEmpty()) { "Foto tidak ditemukan." }
            val selectedMemoryIds = selectedMemories.map(MemoryEntity::id).toSet()
            val mediaObjects = mediaObjectDao.findAll(ownerId)
                .filter { it.memoryId in selectedMemoryIds }
            val selectedAlbums = when {
                albumId != null -> albumDao.findById(albumId)?.takeIf { it.ownerId == ownerId }?.let(::listOf).orEmpty()
                else -> albumDao.findAllForOwner(ownerId).filter { it.deletedAtEpochMs == null }
            }
            val signedUrls = cloudSource?.signedUrls(mediaObjects.mapNotNull { it.storagePath }).orEmpty()

            contentResolver.openOutputStream(destination)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zip ->
                    writeMetadata(zip, selectedAlbums, selectedMemories, mediaObjects)
                    val usedNames = mutableSetOf<String>()
                    mediaObjects.forEach { media ->
                        val source = media.localUri?.let(Uri::parse)?.let(uriResolver::openInputStream)
                            ?: media.storagePath?.let(signedUrls::get)?.let { URL(it).openStream() }
                        source?.use { input ->
                            zip.putNextEntry(ZipEntry("hushkeep-export/photos/${uniqueFileName(media, usedNames)}"))
                            input.copyTo(zip)
                            zip.closeEntry()
                        }
                    }
                }
            } ?: error("Tidak dapat menulis file export.")
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { error -> AppResult.Failure(AppError.Storage("Export gagal.", error)) },
        )
    }

    private fun writeMetadata(
        zip: ZipOutputStream,
        albums: List<AlbumEntity>,
        memories: List<MemoryEntity>,
        mediaObjects: List<MediaObjectEntity>,
    ) {
        val albumJson = JSONArray().apply {
            albums.forEach { album ->
                put(JSONObject().apply {
                    put("id", album.id)
                    put("name", album.name)
                    put("createdAt", Instant.ofEpochMilli(album.createdAtEpochMs).toString())
                    put("updatedAt", Instant.ofEpochMilli(album.updatedAtEpochMs).toString())
                })
            }
        }
        val memoryJson = JSONArray().apply {
            memories.forEach { memory ->
                put(JSONObject().apply {
                    put("id", memory.id)
                    put("albumId", memory.albumId)
                    put("caption", memory.caption)
                    put("capturedAt", Instant.ofEpochMilli(memory.capturedAtEpochMs).toString())
                    put("isFavorite", memory.isFavorite)
                    put("media", JSONArray().apply {
                        mediaObjects.filter { it.memoryId == memory.id }.forEach { media ->
                            put(JSONObject().apply {
                                put("id", media.id)
                                put("fileName", media.fileName)
                                put("mimeType", media.mimeType)
                                put("sizeBytes", media.sizeBytes)
                            })
                        }
                    })
                })
            }
        }
        val metadata = JSONObject()
            .put("format", "hushkeep-export-v1")
            .put("createdAt", Instant.now().toString())
            .put("albums", albumJson)
            .put("memories", memoryJson)
        zip.putNextEntry(ZipEntry("hushkeep-export/metadata.json"))
        zip.write(metadata.toString(2).toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun uniqueFileName(media: MediaObjectEntity, usedNames: MutableSet<String>): String {
        val base = media.fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "${media.id}.bin" }
        var candidate = base
        var suffix = 1
        while (!usedNames.add(candidate)) {
            candidate = "${media.id}-$suffix-$base"
            suffix++
        }
        return candidate
    }
}
