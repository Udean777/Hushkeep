package com.ssajudn.hushkeep.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ssajudn.hushkeep.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query(
        """
        SELECT * FROM memories
        WHERE ownerId = :ownerId AND deletedAtEpochMs IS NULL
        ORDER BY capturedAtEpochMs DESC, createdAtEpochMs DESC
        """,
    )
    fun observeTimeline(ownerId: String): Flow<List<MemoryEntity>>

    @Query(
        """
        SELECT * FROM memories
        WHERE ownerId = :ownerId AND albumId = :albumId AND deletedAtEpochMs IS NULL
        ORDER BY capturedAtEpochMs DESC, createdAtEpochMs DESC
        """,
    )
    fun observeByAlbum(ownerId: String, albumId: String): Flow<List<MemoryEntity>>

    @Query(
        """
        SELECT * FROM memories
        WHERE ownerId = :ownerId AND isFavorite = 1 AND deletedAtEpochMs IS NULL
        ORDER BY capturedAtEpochMs DESC, createdAtEpochMs DESC
        """,
    )
    fun observeFavorites(ownerId: String): Flow<List<MemoryEntity>>

    @Query(
        """
        SELECT * FROM memories
        WHERE ownerId = :ownerId
          AND deletedAtEpochMs IS NULL
          AND (:query = '' OR caption LIKE '%' || :query || '%')
          AND (:albumId IS NULL OR albumId = :albumId)
          AND (:fromEpochMs IS NULL OR capturedAtEpochMs >= :fromEpochMs)
          AND (:toEpochMsExclusive IS NULL OR capturedAtEpochMs < :toEpochMsExclusive)
        ORDER BY capturedAtEpochMs DESC, createdAtEpochMs DESC
        """,
    )
    fun search(
        ownerId: String,
        query: String,
        albumId: String?,
        fromEpochMs: Long?,
        toEpochMsExclusive: Long?,
    ): Flow<List<MemoryEntity>>

    @Query(
        """
        SELECT * FROM memories
        WHERE ownerId = :ownerId AND deletedAtEpochMs IS NOT NULL
        ORDER BY deletedAtEpochMs DESC
        """,
    )
    fun observeTrash(ownerId: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE ownerId = :ownerId")
    suspend fun findAllForOwner(ownerId: String): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE id = :memoryId LIMIT 1")
    suspend fun findById(memoryId: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE ownerId = :ownerId AND albumId = :albumId")
    suspend fun findAllForAlbum(ownerId: String, albumId: String): List<MemoryEntity>

    @Upsert
    suspend fun upsert(memory: MemoryEntity)

    @Upsert
    suspend fun upsertAll(memories: List<MemoryEntity>)

    @Query(
        """
        UPDATE memories
        SET isFavorite = :isFavorite, updatedAtEpochMs = :updatedAtEpochMs,
            syncState = 'PENDING'
        WHERE id = :memoryId AND ownerId = :ownerId
        """,
    )
    suspend fun setFavorite(
        memoryId: String,
        ownerId: String,
        isFavorite: Boolean,
        updatedAtEpochMs: Long,
    ): Int

    @Query(
        """
        UPDATE memories
        SET caption = :caption, updatedAtEpochMs = :updatedAtEpochMs, syncState = 'PENDING'
        WHERE id = :memoryId AND ownerId = :ownerId
        """,
    )
    suspend fun updateCaption(
        memoryId: String,
        ownerId: String,
        caption: String?,
        updatedAtEpochMs: Long,
    ): Int

    @Query(
        """
        UPDATE memories
        SET albumId = NULL, updatedAtEpochMs = :updatedAtEpochMs, syncState = 'PENDING'
        WHERE ownerId = :ownerId AND albumId = :albumId
        """,
    )
    suspend fun detachFromAlbum(ownerId: String, albumId: String, updatedAtEpochMs: Long): Int

    @Query(
        """
        UPDATE memories
        SET syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs
        WHERE id = :memoryId AND ownerId = :ownerId
        """
    )
    suspend fun updateSyncState(
        memoryId: String,
        ownerId: String,
        syncState: String,
        updatedAtEpochMs: Long,
    ): Int

    @Query(
        """
        UPDATE memories
        SET syncState = 'SYNCED'
        WHERE ownerId = :ownerId AND id IN (:memoryIds)
        """
    )
    suspend fun markSynced(ownerId: String, memoryIds: List<String>): Int

    @Query(
        """
        UPDATE memories
        SET syncState = 'PARTIALLY_SYNCED'
        WHERE ownerId = :ownerId
          AND id IN (:memoryIds)
          AND syncState IN ('PENDING', 'SYNCING', 'PARTIALLY_SYNCED', 'FAILED')
        """,
    )
    suspend fun markPartiallySynced(ownerId: String, memoryIds: List<String>): Int

    @Query(
        """
        UPDATE memories
        SET syncState = 'FAILED'
        WHERE ownerId = :ownerId
          AND id IN (:memoryIds)
          AND syncState IN ('PENDING', 'SYNCING', 'PARTIALLY_SYNCED', 'FAILED')
        """,
    )
    suspend fun markSyncFailed(ownerId: String, memoryIds: List<String>): Int

    @Query(
        """
        UPDATE memories
        SET deletedAtEpochMs = :deletedAtEpochMs, updatedAtEpochMs = :updatedAtEpochMs,
            syncState = 'PENDING'
        WHERE id = :memoryId AND ownerId = :ownerId
        """,
    )
    suspend fun softDelete(
        memoryId: String,
        ownerId: String,
        deletedAtEpochMs: Long,
        updatedAtEpochMs: Long,
    ): Int

    @Query(
        """
        UPDATE memories
        SET deletedAtEpochMs = NULL, updatedAtEpochMs = :updatedAtEpochMs,
            syncState = 'PENDING'
        WHERE id = :memoryId AND ownerId = :ownerId
        """,
    )
    suspend fun restore(memoryId: String, ownerId: String, updatedAtEpochMs: Long): Int

    @Query("DELETE FROM memories WHERE id = :memoryId AND ownerId = :ownerId")
    suspend fun permanentlyDelete(memoryId: String, ownerId: String): Int

    @Query("DELETE FROM memories WHERE ownerId = :ownerId")
    suspend fun deleteAllForOwner(ownerId: String): Int
}
