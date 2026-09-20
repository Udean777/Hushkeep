package com.ssajudn.hushkeep.data.remote

import com.ssajudn.hushkeep.data.local.entity.AlbumEntity
import com.ssajudn.hushkeep.data.local.entity.MediaObjectEntity
import com.ssajudn.hushkeep.data.local.entity.MemoryEntity
import com.ssajudn.hushkeep.core.config.StoragePaths
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

@Serializable
data class RemoteAlbum(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class RemoteMemory(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("album_id") val albumId: String? = null,
    val caption: String? = null,
    @SerialName("captured_at") val capturedAt: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("is_favorite") val isFavorite: Boolean,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class RemoteMediaObject(
    val id: String,
    @SerialName("memory_id") val memoryId: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("encryption_version") val encryptionVersion: Int = 0,
    @SerialName("key_id") val keyId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

class SupabaseMemoryDataSource(
    private val client: SupabaseClient,
) {
    suspend fun upsertAlbums(albums: List<AlbumEntity>) {
        if (albums.isEmpty()) return
        client.from("albums").upsert(albums.map { it.toRemote() }) {
            onConflict = "id"
        }
    }

    suspend fun upsertMemories(memories: List<MemoryEntity>) {
        if (memories.isEmpty()) return
        client.from("memories").upsert(memories.map { it.toRemote() }) {
            onConflict = "id"
        }
    }

    suspend fun upsertMedia(mediaObjects: List<MediaObjectEntity>) {
        val ready = mediaObjects.filter { it.storagePath != null }
        if (ready.isEmpty()) return
        client.from("media_objects").upsert(ready.map { it.toRemote() }) {
            onConflict = "id"
        }
    }

    suspend fun fetchAlbums(ownerId: String): List<RemoteAlbum> =
        client.from("albums").select {
            filter { eq("owner_id", ownerId) }
        }.decodeList()

    suspend fun fetchMemories(ownerId: String): List<RemoteMemory> =
        client.from("memories").select {
            filter { eq("owner_id", ownerId) }
        }.decodeList()

    suspend fun fetchMedia(ownerId: String): List<RemoteMediaObject> =
        client.from("media_objects").select {
            filter { eq("owner_id", ownerId) }
        }.decodeList()

    suspend fun signedUrls(paths: Collection<String>): Map<String, String> {
        if (paths.isEmpty()) return emptyMap()
        return client.storage.from(StoragePaths.PRIVATE_BUCKET)
            .createSignedUrls(30.minutes, paths)
            .associate { it.path to it.signedURL }
    }

    suspend fun deleteObjects(paths: Collection<String>) {
        if (paths.isNotEmpty()) client.storage.from(StoragePaths.PRIVATE_BUCKET).delete(paths)
    }

    suspend fun deleteMemory(ownerId: String, memoryId: String) {
        client.from("memories").delete {
            filter {
                eq("id", memoryId)
                eq("owner_id", ownerId)
            }
        }
    }

    suspend fun deleteAlbum(ownerId: String, albumId: String) {
        client.from("albums").delete {
            filter {
                eq("id", albumId)
                eq("owner_id", ownerId)
            }
        }
    }
}

private fun AlbumEntity.toRemote() = RemoteAlbum(
    id = id,
    ownerId = ownerId,
    name = name,
    createdAt = createdAtEpochMs.toInstantString(),
    updatedAt = updatedAtEpochMs.toInstantString(),
    deletedAt = deletedAtEpochMs?.toInstantString(),
)

private fun MemoryEntity.toRemote() = RemoteMemory(
    id = id,
    ownerId = ownerId,
    albumId = albumId,
    caption = caption,
    capturedAt = capturedAtEpochMs.toInstantString(),
    createdAt = createdAtEpochMs.toInstantString(),
    updatedAt = updatedAtEpochMs.toInstantString(),
    isFavorite = isFavorite,
    deletedAt = deletedAtEpochMs?.toInstantString(),
)

private fun MediaObjectEntity.toRemote() = RemoteMediaObject(
    id = id,
    memoryId = memoryId,
    ownerId = ownerId,
    fileName = fileName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    storagePath = storagePath!!,
    createdAt = createdAtEpochMs.toInstantString(),
    updatedAt = updatedAtEpochMs.toInstantString(),
)

private fun Long.toInstantString(): String =
    java.time.Instant.ofEpochMilli(this).toString()
