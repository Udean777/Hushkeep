package com.ssajudn.hushkeep.core.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the local Room cache in sync with remote changes while the user is
 * signed in. Room remains the only source consumed by Compose screens.
 *
 * A database event triggers one coalesced repository refresh. This keeps the
 * realtime payload parsing in one place and lets the repository continue to
 * apply its conflict and signed-URL rules consistently.
 */
interface RealtimeSyncCoordinator {
    suspend fun start(ownerId: String)

    suspend fun stop()

    fun close()
}

@OptIn(FlowPreview::class)
class SupabaseRealtimeSyncCoordinator(
    private val client: SupabaseClient?,
    private val onRemoteChange: suspend (ownerId: String) -> Unit,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : RealtimeSyncCoordinator {
    private val lifecycleMutex = Mutex()
    private val refreshMutex = Mutex()

    private var activeOwnerId: String? = null
    private var channel: RealtimeChannel? = null
    private var runtimeScope: CoroutineScope? = null
    private var eventJob: Job? = null
    private var refreshJob: Job? = null

    override suspend fun start(ownerId: String) {
        lifecycleMutex.withLock {
            if (activeOwnerId == ownerId && runtimeScope != null) return

            stopLocked()
            val supabase = client ?: return
            val scope = CoroutineScope(SupervisorJob() + dispatcher)
            val realtimeChannel = supabase.channel("hushkeep-memory-$ownerId")
            val refreshRequests = MutableSharedFlow<Unit>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
            val changeFlows = listOf(
                realtimeChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "memories"
                    filter("owner_id", FilterOperator.EQ, ownerId)
                },
                realtimeChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "media_objects"
                    filter("owner_id", FilterOperator.EQ, ownerId)
                },
                realtimeChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "albums"
                    filter("owner_id", FilterOperator.EQ, ownerId)
                },
            )

            activeOwnerId = ownerId
            channel = realtimeChannel
            runtimeScope = scope
            eventJob = scope.launch {
                merge(*changeFlows.toTypedArray())
                    .catch { /* Realtime handles reconnecting; keep UI state local. */ }
                    .collect { _: PostgresAction -> refreshRequests.tryEmit(Unit) }
            }
            refreshJob = scope.launch {
                refreshRequests
                    .debounce(250)
                    .collect {
                        refreshMutex.withLock {
                            try {
                                onRemoteChange(ownerId)
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Throwable) {
                                // A transient refresh failure must not stop future events.
                            }
                        }
                    }
            }

            // The channel subscribes after all change flows have been registered.
            try {
                realtimeChannel.subscribe()
            } catch (_: Throwable) {
                stopLocked()
            }
        }
    }

    override suspend fun stop() {
        lifecycleMutex.withLock { stopLocked() }
    }

    override fun close() {
        runtimeScope?.cancel()
        activeOwnerId = null
        channel = null
        runtimeScope = null
        eventJob = null
        refreshJob = null
    }

    private suspend fun stopLocked() {
        eventJob?.cancel()
        refreshJob?.cancel()
        channel?.unsubscribe()
        runtimeScope?.cancel()
        activeOwnerId = null
        channel = null
        runtimeScope = null
        eventJob = null
        refreshJob = null
    }
}
