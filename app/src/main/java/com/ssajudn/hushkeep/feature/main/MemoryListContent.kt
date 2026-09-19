package com.ssajudn.hushkeep.feature.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssajudn.hushkeep.core.common.HushkeepDateTimeFormatter
import com.ssajudn.hushkeep.core.common.UiState
import com.ssajudn.hushkeep.core.ui.components.ArchiveDateLabel
import com.ssajudn.hushkeep.core.ui.components.EmptyState
import com.ssajudn.hushkeep.core.ui.components.ErrorState
import com.ssajudn.hushkeep.core.ui.components.FeaturedMemoryCard
import com.ssajudn.hushkeep.core.ui.components.LoadingState
import com.ssajudn.hushkeep.core.ui.components.MemoryGridTile
import com.ssajudn.hushkeep.core.ui.components.MemoryListItem
import com.ssajudn.hushkeep.domain.model.Memory

@Composable
fun MemoryListContent(
    state: UiState<List<Memory>>,
    onFavorite: (Memory) -> Unit,
    onDelete: (Memory) -> Unit,
    onOpen: (Memory) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        UiState.Loading -> LoadingState(modifier)
        UiState.Empty -> EmptyState(
            title = "Belum ada kenangan",
            description = "Pilih foto dari galeri untuk mulai mengisi ruang pribadi ini.",
            modifier = modifier,
        )
        is UiState.Error -> ErrorState(
            message = "Kenangan belum dapat dimuat.",
            modifier = modifier,
        )
        is UiState.Content -> {
            val groups = state.value.groupBy { memory ->
                HushkeepDateTimeFormatter.timelineDay(memory.capturedAt)
            }
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                groups.entries.forEachIndexed { groupIndex, (date, memories) ->
                    item(key = "date-$date") {
                        ArchiveDateLabel(
                            date,
                            modifier = Modifier.padding(top = if (groupIndex == 0) 0.dp else 16.dp),
                        )
                    }
                    if (groupIndex == 0 && memories.isNotEmpty()) {
                        item(key = "featured-${memories.first().id}") {
                            FeaturedMemoryCard(
                                memory = memories.first(),
                                onFavorite = { onFavorite(memories.first()) },
                                onDelete = { onDelete(memories.first()) },
                                onOpen = { onOpen(memories.first()) },
                            )
                        }
                    }
                    val remaining = if (groupIndex == 0) memories.drop(1) else memories
                    remaining.chunked(2).forEachIndexed { rowIndex, row ->
                        item(key = "row-$date-$rowIndex") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                row.forEach { memory ->
                                    MemoryGridTile(
                                        memory = memory,
                                        onFavorite = { onFavorite(memory) },
                                        onDelete = { onDelete(memory) },
                                        onOpen = { onOpen(memory) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
