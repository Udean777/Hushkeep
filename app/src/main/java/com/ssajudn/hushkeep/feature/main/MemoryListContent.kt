/*
 * Hallmark · genre: playful editorial · macrostructure: Workbench · theme: bright memory archive
 * designed-as-app · tone: gallery / canvas-first · motion: restrained
 * pre-emit critique: P5 H5 E4 S5 R4 V5
 */
package com.ssajudn.hushkeep.feature.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.ssajudn.hushkeep.core.ui.components.MemoryResurfacingLane
import com.ssajudn.hushkeep.domain.model.Memory
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun MemoryListContent(
    state: UiState<List<Memory>>,
    onFavorite: (Memory) -> Unit,
    onDelete: (Memory) -> Unit,
    onOpen: (Memory) -> Unit,
    emptyTitle: String = "Belum ada kenangan",
    emptyDescription: String = "Pilih foto dari galeri untuk mulai mengisi ruang pribadi ini.",
    modifier: Modifier = Modifier,
) {
    when (state) {
        UiState.Loading -> LoadingState(modifier)
        UiState.Empty -> EmptyState(
            title = emptyTitle,
            description = emptyDescription,
            modifier = modifier,
        )
        is UiState.Error -> ErrorState(
            message = "Kenangan belum dapat dimuat.",
            modifier = modifier,
        )
        is UiState.Content -> BoxWithConstraints(
            modifier = modifier.fillMaxSize(),
        ) {
            val columns = if (maxWidth >= 720.dp) 3 else 2
            val horizontalPadding = if (maxWidth >= 840.dp) 36.dp else 22.dp
            val groups = state.value.groupBy { memory ->
                HushkeepDateTimeFormatter.timelineDay(memory.capturedAt)
            }
            val resurfaced = state.value
                .filter { it.isFromAnEarlierYearOnThisDay() }
                .take(6)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    top = 18.dp,
                    end = horizontalPadding,
                    bottom = 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (resurfaced.isNotEmpty()) {
                    item(key = "resurfacing") {
                        MemoryResurfacingLane(
                            memories = resurfaced,
                            onOpen = onOpen,
                        )
                    }
                }

                groups.entries.forEachIndexed { groupIndex, (date, memories) ->
                    item(key = "date-$date") {
                        ArchiveDateLabel(
                            date,
                            modifier = Modifier.padding(top = if (groupIndex == 0) 4.dp else 20.dp),
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
                    remaining.chunked(columns).forEachIndexed { rowIndex, row ->
                        item(key = "row-$date-$rowIndex") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                row.forEachIndexed { tileIndex, memory ->
                                    MemoryGridTile(
                                        memory = memory,
                                        onFavorite = { onFavorite(memory) },
                                        onOpen = { onOpen(memory) },
                                        aspectRatio = tileAspectRatio(rowIndex, tileIndex),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(columns - row.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun tileAspectRatio(rowIndex: Int, tileIndex: Int): Float =
    when ((rowIndex + tileIndex) % 3) {
        0 -> 0.88f
        1 -> 1f
        else -> 1.12f
    }

private fun Memory.isFromAnEarlierYearOnThisDay(
    today: LocalDate = LocalDate.now(),
): Boolean {
    val capturedDate = capturedAt.atZone(ZoneId.systemDefault()).toLocalDate()
    return capturedDate.year < today.year &&
        capturedDate.month == today.month &&
        capturedDate.dayOfMonth == today.dayOfMonth
}
