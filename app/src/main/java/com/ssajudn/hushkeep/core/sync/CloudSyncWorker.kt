package com.ssajudn.hushkeep.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.ssajudn.hushkeep.core.common.AppResult
import com.ssajudn.hushkeep.core.network.SupabaseClientProvider
import com.ssajudn.hushkeep.data.local.HushkeepDatabase
import com.ssajudn.hushkeep.data.remote.SupabaseMemoryDataSource
import com.ssajudn.hushkeep.data.repository.LocalMemoryRepository
import com.ssajudn.hushkeep.domain.model.SyncState
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime

class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val ownerId = inputData.getString(CloudSyncWorkRequestFactory.OWNER_ID_KEY)
            ?: return Result.failure()
        val client = SupabaseClientProvider.create() ?: return Result.success()
        val database = HushkeepDatabase.getInstance(applicationContext)
        val source = SupabaseMemoryDataSource(client)
        var attemptedMemoryIds = emptySet<String>()

        return runCatching {
            // Pull remote-newer records before deciding what this device should
            // push. This keeps background sync consistent with app-resume sync.
            val refreshResult = LocalMemoryRepository(
                database = database,
                contentResolver = applicationContext.contentResolver,
                mediaDirectory = File(applicationContext.filesDir, "hushkeep-media"),
                workManager = WorkManager.getInstance(applicationContext),
                supabaseClient = client,
            ).refreshFromCloud(ownerId)
            check(refreshResult !is AppResult.Failure) { "Cloud refresh failed." }

            val albums = database.albumDao().findAllForOwner(ownerId)
            val memories = database.memoryDao().findAllForOwner(ownerId)
            val mediaObjects = database.mediaObjectDao().findAll(ownerId)
            val remoteAlbums = source.fetchAlbums(ownerId).associateBy { it.id }
            val remoteMemories = source.fetchMemories(ownerId).associateBy { it.id }
            val remoteMedia = source.fetchMedia(ownerId).associateBy { it.id }
            val albumsToPush = albums.filter { local ->
                val remote = remoteAlbums[local.id]
                remote == null || SyncConflictResolver.shouldUploadLocal(
                    local.updatedAtEpochMs,
                    remote.updatedAt.toEpochMs(),
                )
            }
            val memoriesToPush = memories.filter { local ->
                val remote = remoteMemories[local.id]
                remote == null || SyncConflictResolver.shouldUploadLocal(
                    local.updatedAtEpochMs,
                    remote.updatedAt.toEpochMs(),
                    local.syncState,
                )
            }
            val mediaToPush = mediaObjects.filter { local ->
                val remote = remoteMedia[local.id]
                remote == null || SyncConflictResolver.shouldUploadLocal(
                    local.updatedAtEpochMs,
                    remote.updatedAt.toEpochMs(),
                    local.syncState,
                )
            }
            attemptedMemoryIds = buildSet {
                addAll(memoriesToPush.map { it.id })
                addAll(mediaToPush.map { it.memoryId })
            }
            source.upsertAlbums(albumsToPush)
            source.upsertMemories(memoriesToPush)
            source.upsertMedia(mediaToPush)

            val mediaByMemory = mediaObjects.groupBy { it.memoryId }
            val fullyBackedUpMemoryIds = memoriesToPush
                .filter { mediaByMemory[it.id].orEmpty().all { media -> media.storagePath != null } }
                .map { it.id }
            if (fullyBackedUpMemoryIds.isNotEmpty()) {
                database.memoryDao().markSynced(ownerId, fullyBackedUpMemoryIds)
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                val database = HushkeepDatabase.getInstance(applicationContext)
                val memories = database.memoryDao().findAllForOwner(ownerId)
                val mediaByMemory = database.mediaObjectDao()
                    .findAll(ownerId)
                    .groupBy { it.memoryId }
                val retryableMemories = memories.filter { memory ->
                    val state = SyncState.fromStorage(memory.syncState)
                    memory.id in attemptedMemoryIds && SyncStatePolicy.isRetryable(state.name)
                }
                val partiallySyncedIds = retryableMemories
                    .filter { memory ->
                        SyncStatePolicy.afterMetadataSyncFailure(
                            hasUploadedMedia = mediaByMemory[memory.id]
                                .orEmpty()
                                .any { it.storagePath != null },
                        ) == SyncState.PARTIALLY_SYNCED
                    }
                    .map { it.id }
                val failedIds = retryableMemories
                    .filterNot { it.id in partiallySyncedIds }
                    .map { it.id }
                database.memoryDao().markPartiallySynced(ownerId, partiallySyncedIds)
                database.memoryDao().markSyncFailed(ownerId, failedIds)
                Result.retry()
            },
        )
    }

    private fun String.toEpochMs(): Long = runCatching {
        Instant.parse(this).toEpochMilli()
    }.getOrElse {
        OffsetDateTime.parse(this).toInstant().toEpochMilli()
    }
}
