package com.ssajudn.hushkeep.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ssajudn.hushkeep.data.local.entity.AlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query(
        """
        SELECT * FROM albums
        WHERE ownerId = :ownerId AND deletedAtEpochMs IS NULL
        ORDER BY updatedAtEpochMs DESC
        """,
    )
    fun observeActive(ownerId: String): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :albumId LIMIT 1")
    suspend fun findById(albumId: String): AlbumEntity?

    @Query(
        """
        SELECT * FROM albums
        WHERE ownerId = :ownerId AND deletedAtEpochMs IS NOT NULL
        ORDER BY deletedAtEpochMs DESC
        """,
    )
    fun observeTrash(ownerId: String): Flow<List<AlbumEntity>>

    @Upsert
    suspend fun upsert(album: AlbumEntity)

    @Upsert
    suspend fun upsertAll(albums: List<AlbumEntity>)

    @Query(
        """
        UPDATE albums
        SET deletedAtEpochMs = :deletedAtEpochMs, updatedAtEpochMs = :updatedAtEpochMs
        WHERE id = :albumId AND ownerId = :ownerId
        """,
    )
    suspend fun softDelete(
        albumId: String,
        ownerId: String,
        deletedAtEpochMs: Long,
        updatedAtEpochMs: Long,
    ): Int

    @Query(
        """
        UPDATE albums
        SET deletedAtEpochMs = NULL, updatedAtEpochMs = :updatedAtEpochMs
        WHERE id = :albumId AND ownerId = :ownerId
        """,
    )
    suspend fun restore(albumId: String, ownerId: String, updatedAtEpochMs: Long): Int

    @Query("DELETE FROM albums WHERE id = :albumId AND ownerId = :ownerId")
    suspend fun permanentlyDelete(albumId: String, ownerId: String): Int
}
