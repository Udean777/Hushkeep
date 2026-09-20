package com.ssajudn.hushkeep.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ssajudn.hushkeep.data.local.entity.MediaObjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaObjectDao {
    @Query("SELECT * FROM media_objects WHERE id = :mediaObjectId LIMIT 1")
    suspend fun findById(mediaObjectId: String): MediaObjectEntity?

    @Query("SELECT * FROM media_objects WHERE ownerId = :ownerId ORDER BY createdAtEpochMs ASC")
    suspend fun findAll(ownerId: String): List<MediaObjectEntity>

    @Query("SELECT * FROM media_objects WHERE ownerId = :ownerId AND storagePath IS NOT NULL")
    suspend fun findSyncedForOwner(ownerId: String): List<MediaObjectEntity>

    @Query("SELECT * FROM media_objects WHERE memoryId = :memoryId")
    suspend fun findForMemory(memoryId: String): List<MediaObjectEntity>

    @Query(
        """
        SELECT * FROM media_objects
        WHERE memoryId = :memoryId
        ORDER BY createdAtEpochMs ASC
        """,
    )
    fun observeForMemory(memoryId: String): Flow<List<MediaObjectEntity>>

    @Query(
        """
        UPDATE media_objects
        SET syncState = :syncState, storagePath = :storagePath, updatedAtEpochMs = :updatedAtEpochMs
        WHERE id = :mediaObjectId
        """,
    )
    suspend fun updateSyncState(
        mediaObjectId: String,
        syncState: String,
        storagePath: String?,
        updatedAtEpochMs: Long,
    ): Int

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media_objects WHERE ownerId = :ownerId AND localUri IS NOT NULL")
    fun observeLocalStorageUsage(ownerId: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media_objects WHERE ownerId = :ownerId AND storagePath IS NOT NULL")
    fun observeCloudStorageUsage(ownerId: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media_objects WHERE ownerId = :ownerId AND storagePath IS NULL")
    fun observePendingStorageUsage(ownerId: String): Flow<Long>

    @Query("DELETE FROM media_objects WHERE memoryId = :memoryId")
    suspend fun deleteForMemory(memoryId: String): Int

    @Query("DELETE FROM media_objects WHERE ownerId = :ownerId")
    suspend fun deleteAllForOwner(ownerId: String): Int

    @Upsert
    suspend fun upsert(mediaObject: MediaObjectEntity)

    @Upsert
    suspend fun upsertAll(mediaObjects: List<MediaObjectEntity>)
}
