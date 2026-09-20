package com.ssajudn.hushkeep.feature.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ssajudn.hushkeep.core.common.UiState
import com.ssajudn.hushkeep.core.common.HushkeepDateTimeFormatter
import com.ssajudn.hushkeep.core.ui.components.AlbumCard
import com.ssajudn.hushkeep.core.ui.components.ArchiveAddButton
import com.ssajudn.hushkeep.core.ui.components.ArchiveHeader
import com.ssajudn.hushkeep.core.ui.components.ConfirmDestructiveDialog
import com.ssajudn.hushkeep.core.ui.components.EmptyState
import com.ssajudn.hushkeep.core.ui.components.ErrorState
import com.ssajudn.hushkeep.core.ui.components.HushkeepTopBar
import com.ssajudn.hushkeep.core.ui.components.LoadingState
import com.ssajudn.hushkeep.core.ui.components.UploadProgressRow
import com.ssajudn.hushkeep.core.ui.components.displayUri
import com.ssajudn.hushkeep.domain.model.Album
import com.ssajudn.hushkeep.domain.model.Memory
import com.ssajudn.hushkeep.domain.model.MemorySearchFilters
import com.ssajudn.hushkeep.domain.model.PhotoImport
import com.ssajudn.hushkeep.domain.model.SyncState
import com.ssajudn.hushkeep.domain.model.TrashItem
import com.ssajudn.hushkeep.domain.model.TrashResourceType
import com.ssajudn.hushkeep.feature.HushkeepViewModel
import com.ssajudn.hushkeep.ui.theme.HushkeepNight
import com.ssajudn.hushkeep.ui.theme.HushkeepNightLine
import com.ssajudn.hushkeep.ui.theme.HushkeepNightMuted
import com.ssajudn.hushkeep.ui.theme.HushkeepNightSurface
import com.ssajudn.hushkeep.ui.theme.HushkeepCoralLight
import com.ssajudn.hushkeep.ui.theme.HushkeepPaper
import com.ssajudn.hushkeep.ui.theme.HushkeepSheetShape
import java.time.Instant
import java.time.ZoneId

@Composable
fun TimelineScreen(viewModel: HushkeepViewModel) {
    var selectedMemory by remember { mutableStateOf<Memory?>(null) }
    var pendingPhotos by remember { mutableStateOf<List<PhotoImportDraft>>(emptyList()) }
    var exportTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    val timelineState by viewModel.timeline.collectAsStateWithLifecycle()
    val uploadJobs by viewModel.uploadJobs.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) pendingPhotos = uris.map(::PhotoImportDraft)
    }
    val exportLauncher = rememberLauncherForActivityResult(CreateDocument("image/*")) { uri ->
        val memoryId = exportTargetId
        if (uri != null && memoryId != null) viewModel.downloadPhoto(memoryId, uri)
        exportTargetId = null
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HushkeepTopBar("Vault")
            ArchiveHeader(
                eyebrow = "YOUR QUIET SPACE",
                title = "Keep the good days close.",
                modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 12.dp),
            )
            AnimatedVisibility(
                visible = uploadJobs.isNotEmpty(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
            ) {
                UploadProgressRow(
                    jobs = uploadJobs,
                    onRetry = viewModel::retryUpload,
                    onRetryAll = viewModel::retryFailedUploads,
                    onCancel = viewModel::cancelUpload,
                )
            }
            MemoryListContent(
                state = timelineState,
                onFavorite = viewModel::toggleFavorite,
                onDelete = viewModel::deleteMemory,
                onOpen = { selectedMemory = it },
                modifier = Modifier.weight(1f),
            )
        }
        ArchiveAddButton(
            label = "Keep a memory",
            onClick = {
                picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }

    if (pendingPhotos.isNotEmpty()) {
        PhotoImportDialog(
            initialPhotos = pendingPhotos,
            destinationLabel = "Vault",
            onDismiss = { pendingPhotos = emptyList() },
            onConfirm = { photos ->
                viewModel.importPhotos(photos.map { PhotoImport(it.uri, it.caption) })
                pendingPhotos = emptyList()
            },
        )
    }

    selectedMemory?.let { memory ->
        MemoryViewerDialog(
            memory = memory,
            onDismiss = { selectedMemory = null },
            onFavorite = { viewModel.toggleFavorite(memory) },
            onDelete = {
                viewModel.deleteMemory(memory)
                selectedMemory = null
            },
            onSaveCaption = { caption -> viewModel.updateCaption(memory, caption) },
            onExport = {
                exportTargetId = memory.id
                exportLauncher.launch("hushkeep-${memory.id}.jpg")
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: HushkeepViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    var fromDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var toDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateFilter by remember { mutableStateOf<DateFilter?>(null) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var selectedMemory by remember { mutableStateOf<Memory?>(null) }
    val albumsState by viewModel.albums.collectAsStateWithLifecycle()
    val filters = remember(query, selectedAlbumId, fromDateMillis, toDateMillis) {
        MemorySearchFilters(
            query = query,
            albumId = selectedAlbumId,
            fromEpochMs = fromDateMillis?.let(::startOfDayEpochMs),
            toEpochMsExclusive = toDateMillis?.let(::startOfNextDayEpochMs),
        )
    }
    val state by remember(filters) { viewModel.search(filters) }
        .collectAsStateWithLifecycle(initialValue = UiState.Loading)
    val albums = (albumsState as? UiState.Content)?.value.orEmpty()
    val activeFilterCount = listOf(
        selectedAlbumId != null,
        fromDateMillis != null,
        toDateMillis != null,
    ).count { it }

    Column(Modifier.fillMaxSize()) {
        HushkeepTopBar("Search")
        ArchiveHeader(
            eyebrow = "YOUR ARCHIVE",
            title = "Find a moment",
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                placeholder = { Text("Cari caption atau kata kunci") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (query.isNotBlank()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Bersihkan pencarian")
                        }
                    }
                } else {
                    null
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            FilledTonalButton(
                onClick = { showFilters = true },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Default.Tune, contentDescription = null)
            }
        }
        if (activeFilterCount > 0) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selectedAlbumId?.let { albumId ->
                    val albumName = albums.firstOrNull { it.id == albumId }?.name ?: "Album dipilih"
                    item(key = "active-album") {
                        FilterChip(
                            selected = true,
                            onClick = { selectedAlbumId = null },
                            label = {
                                Text(
                                    albumName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Hapus filter album"
                                )
                            },
                            colors = activeFilterColors(),
                        )
                    }
                }
                fromDateMillis?.let { date ->
                    item(key = "active-from") {
                        FilterChip(
                            selected = true,
                            onClick = { fromDateMillis = null },
                            label = { Text("Dari ${formatFilterDate(date)}") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Hapus tanggal awal"
                                )
                            },
                            colors = activeFilterColors(),
                        )
                    }
                }
                toDateMillis?.let { date ->
                    item(key = "active-to") {
                        FilterChip(
                            selected = true,
                            onClick = { toDateMillis = null },
                            label = { Text("Sampai ${formatFilterDate(date)}") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Hapus tanggal akhir"
                                )
                            },
                            colors = activeFilterColors(),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(if (activeFilterCount > 0) 8.dp else 4.dp))
        MemoryListContent(
            state = state,
            onFavorite = viewModel::toggleFavorite,
            onDelete = viewModel::deleteMemory,
            onOpen = { selectedMemory = it },
            emptyTitle = "Tidak ada hasil",
            emptyDescription = "Coba ubah kata kunci, album, atau rentang tanggal pencarian.",
            modifier = Modifier.weight(1f),
        )
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
        ) {
            SearchFilterSheet(
                albums = albums,
                selectedAlbumId = selectedAlbumId,
                fromDateMillis = fromDateMillis,
                toDateMillis = toDateMillis,
                onAlbumSelected = { selectedAlbumId = it },
                onFromDateClick = { dateFilter = DateFilter.FROM },
                onToDateClick = { dateFilter = DateFilter.TO },
                onClear = {
                    fromDateMillis = null
                    toDateMillis = null
                    selectedAlbumId = null
                },
                onApply = { showFilters = false },
            )
        }
    }

    selectedMemory?.let { memory ->
        MemoryViewerDialog(
            memory = memory,
            onDismiss = { selectedMemory = null },
            onFavorite = { viewModel.toggleFavorite(memory) },
            onDelete = {
                viewModel.deleteMemory(memory)
                selectedMemory = null
            },
            onSaveCaption = { caption -> viewModel.updateCaption(memory, caption) },
        )
    }

    if (dateFilter != null) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { dateFilter = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            if (dateFilter == DateFilter.FROM) fromDateMillis = selected
                            else toDateMillis = selected
                        }
                        dateFilter = null
                    },
                    enabled = datePickerState.selectedDateMillis != null,
                ) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { dateFilter = null }) { Text("Batal") } },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SearchFilterSheet(
    albums: List<Album>,
    selectedAlbumId: String?,
    fromDateMillis: Long?,
    toDateMillis: Long?,
    onAlbumSelected: (String?) -> Unit,
    onFromDateClick: () -> Unit,
    onToDateClick: () -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
) {
    val hasFilters = selectedAlbumId != null || fromDateMillis != null || toDateMillis != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text("Filter pencarian", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Persempit hasil berdasarkan album atau waktu disimpan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(22.dp))
        Text("Album", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(10.dp))
        if (albums.isEmpty()) {
            Text(
                "Belum ada album untuk difilter.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedAlbumId == null,
                        onClick = { onAlbumSelected(null) },
                        label = { Text("Semua album") },
                        colors = archiveFilterColors(),
                    )
                }
                items(albums, key = Album::id) { album ->
                    FilterChip(
                        selected = selectedAlbumId == album.id,
                        onClick = {
                            onAlbumSelected(if (selectedAlbumId == album.id) null else album.id)
                        },
                        label = {
                            Text(
                                album.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = archiveFilterColors(),
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("Rentang tanggal", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onFromDateClick,
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text("Mulai", style = MaterialTheme.typography.labelSmall)
                    Text(
                        fromDateMillis?.let(::formatFilterDate) ?: "Pilih tanggal",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            OutlinedButton(
                onClick = onToDateClick,
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text("Sampai", style = MaterialTheme.typography.labelSmall)
                    Text(
                        toDateMillis?.let(::formatFilterDate) ?: "Pilih tanggal",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onClear,
                enabled = hasFilters,
            ) { Text("Bersihkan") }
            Button(
                onClick = onApply,
                modifier = Modifier.weight(1f),
            ) { Text("Terapkan filter") }
        }
        Spacer(Modifier.height(12.dp))
    }
}

private enum class DateFilter { FROM, TO }

private fun startOfDayEpochMs(epochMs: Long): Long =
    Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

private fun startOfNextDayEpochMs(epochMs: Long): Long =
    Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

private fun formatFilterDate(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()

@Composable
private fun archiveFilterColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
    containerColor = MaterialTheme.colorScheme.surface,
)

@Composable
private fun activeFilterColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Composable
fun FavoritesScreen(viewModel: HushkeepViewModel) {
    val state by viewModel.favorites.collectAsStateWithLifecycle()
    var selectedMemory by remember { mutableStateOf<Memory?>(null) }
    Column(Modifier.fillMaxSize()) {
        HushkeepTopBar("Favorites")
        ArchiveHeader(
            eyebrow = "KEPT CLOSE",
            title = "Saved moments",
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 8.dp),
        )
        MemoryListContent(
            state = state,
            onFavorite = viewModel::toggleFavorite,
            onDelete = viewModel::deleteMemory,
            onOpen = { selectedMemory = it },
            modifier = Modifier.weight(1f),
        )
    }
    selectedMemory?.let { memory ->
        MemoryViewerDialog(
            memory = memory,
            onDismiss = { selectedMemory = null },
            onFavorite = { viewModel.toggleFavorite(memory) },
            onDelete = {
                viewModel.deleteMemory(memory)
                selectedMemory = null
            },
            onSaveCaption = { caption -> viewModel.updateCaption(memory, caption) },
        )
    }
}

@Composable
fun AlbumsScreen(
    viewModel: HushkeepViewModel,
    onOpenAlbum: (String) -> Unit,
) {
    val state by viewModel.albums.collectAsStateWithLifecycle()
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteAlbum by remember { mutableStateOf<Album?>(null) }
    var pendingRenameAlbum by remember { mutableStateOf<Album?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HushkeepTopBar("Albums")
            ArchiveHeader(
                eyebrow = "YOUR SHELVES",
                title = "Private albums",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                action = {
                    TextButton(onClick = { showCreateDialog = true }) { Text("New album") }
                },
            )
            when (val currentState = state) {
                UiState.Loading -> LoadingState(Modifier.weight(1f))
                UiState.Empty -> EmptyState(
                    title = "Belum ada album",
                    description = "Buat ruang kecil untuk cerita yang ingin kamu simpan bersama.",
                    modifier = Modifier.weight(1f),
                    actionLabel = "Buat album",
                    onAction = { showCreateDialog = true },
                )

                is UiState.Error -> ErrorState(
                    message = "Album belum dapat dimuat.",
                    modifier = Modifier.weight(1f),
                )

                is UiState.Content -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(currentState.value, key = Album::id) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onOpenAlbum(album.id) },
                            onDelete = { pendingDeleteAlbum = album },
                            onRename = { pendingRenameAlbum = album },
                        )
                    }
                }
            }
        }
        ArchiveAddButton(
            label = "New album",
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }

    if (showCreateDialog) {
        CreateAlbumDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createAlbum(name)
                showCreateDialog = false
            },
        )
    }

    pendingDeleteAlbum?.let { album ->
        ConfirmDestructiveDialog(
            title = "Pindahkan album ke trash?",
            message = "Foto di dalam album tetap tersimpan di vault.",
            confirmLabel = "Pindahkan",
            onConfirm = {
                viewModel.deleteAlbum(album.id)
                pendingDeleteAlbum = null
            },
            onDismiss = { pendingDeleteAlbum = null },
        )
    }

    pendingRenameAlbum?.let { album ->
        RenameAlbumDialog(
            album = album,
            onDismiss = { pendingRenameAlbum = null },
            onRename = { name ->
                viewModel.renameAlbum(album.id, name)
                pendingRenameAlbum = null
            },
        )
    }
}

@Composable
private fun CreateAlbumDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat album pribadi") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama album") },
                placeholder = { Text("Contoh: Weekend di Bandung") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onCreate(name) }, enabled = name.isNotBlank()) {
                Text("Simpan album")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun RenameAlbumDialog(
    album: Album,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by rememberSaveable(album.id) { mutableStateOf(album.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah nama album") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(120) },
                label = { Text("Nama album") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onRename(name) }, enabled = name.isNotBlank()) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun AlbumDetailScreen(
    albumId: String,
    viewModel: HushkeepViewModel,
    onBack: () -> Unit,
) {
    val state by remember(albumId) { viewModel.observeAlbum(albumId) }
        .collectAsStateWithLifecycle(initialValue = UiState.Loading)
    var selectedMemory by remember { mutableStateOf<Memory?>(null) }
    var pendingPhotos by remember { mutableStateOf<List<PhotoImportDraft>>(emptyList()) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) pendingPhotos = uris.map(::PhotoImportDraft)
    }
    val exportLauncher =
        rememberLauncherForActivityResult(CreateDocument("application/zip")) { uri ->
            if (uri != null) viewModel.exportAlbum(albumId, uri)
        }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HushkeepTopBar(
                title = "Album",
                onBack = onBack,
                actions = {
                    TextButton(onClick = { exportLauncher.launch("hushkeep-album-$albumId.zip") }) {
                        Text("Export")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Pindahkan album ke trash"
                        )
                    }
                },
            )
            ArchiveHeader(
                eyebrow = "PRIVATE ALBUM",
                title = "A small room for memories",
                modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 8.dp),
            )
            MemoryListContent(
                state = state,
                onFavorite = viewModel::toggleFavorite,
                onDelete = viewModel::deleteMemory,
                onOpen = { selectedMemory = it },
                modifier = Modifier.weight(1f),
            )
        }
        ArchiveAddButton(
            label = "Add photo",
            onClick = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }
    if (pendingPhotos.isNotEmpty()) {
        PhotoImportDialog(
            initialPhotos = pendingPhotos,
            destinationLabel = "Album ini",
            onDismiss = { pendingPhotos = emptyList() },
            onConfirm = { photos ->
                viewModel.importPhotos(photos.map { PhotoImport(it.uri, it.caption) }, albumId)
                pendingPhotos = emptyList()
            },
        )
    }
    selectedMemory?.let { memory ->
        MemoryViewerDialog(
            memory = memory,
            onDismiss = { selectedMemory = null },
            onFavorite = { viewModel.toggleFavorite(memory) },
            onDelete = {
                viewModel.deleteMemory(memory)
                selectedMemory = null
            },
            onSaveCaption = { caption -> viewModel.updateCaption(memory, caption) },
        )
    }

    if (showDeleteDialog) {
        ConfirmDestructiveDialog(
            title = "Pindahkan album ke trash?",
            message = "Foto di dalam album tetap tersimpan di vault.",
            confirmLabel = "Pindahkan",
            onConfirm = {
                viewModel.deleteAlbum(albumId)
                showDeleteDialog = false
                onBack()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

private data class PhotoImportDraft(
    val uri: Uri,
    val caption: String = "",
)

@Composable
private fun PhotoImportDialog(
    initialPhotos: List<PhotoImportDraft>,
    destinationLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (List<PhotoImportDraft>) -> Unit,
) {
    var drafts by remember(initialPhotos) { mutableStateOf(initialPhotos) }
    var selectedIndex by remember(initialPhotos) { mutableStateOf(0) }
    val selectedPhoto = drafts[selectedIndex.coerceIn(0, drafts.lastIndex)]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .imePadding()
                .navigationBarsPadding(),
            shape = HushkeepSheetShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text("Siapkan kenangan", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tambahkan caption sebelum foto masuk ke $destinationLabel. Setiap foto bisa punya cerita sendiri.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    itemsIndexed(
                        drafts,
                        key = { index, draft -> "${draft.uri}-$index" }) { index, draft ->
                        val shape = MaterialTheme.shapes.medium
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(shape)
                                .border(
                                    width = if (index == selectedIndex) 3.dp else 1.dp,
                                    color = if (index == selectedIndex) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = shape,
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            AsyncImage(
                                model = draft.uri,
                                contentDescription = "Foto ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            Surface(
                                onClick = { selectedIndex = index },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .semantics { contentDescription = "Pilih foto ${index + 1}" },
                                color = androidx.compose.ui.graphics.Color.Transparent,
                            ) {}
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Foto ${selectedIndex + 1} dari ${drafts.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = selectedPhoto.caption,
                    onValueChange = { value ->
                        drafts = drafts.mapIndexed { index, draft ->
                            if (index == selectedIndex) draft.copy(caption = value.take(280)) else draft
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Caption foto ini") },
                    placeholder = { Text("Contoh: Sore yang berjalan pelan") },
                    minLines = 3,
                    maxLines = 4,
                    supportingText = { Text("${selectedPhoto.caption.length}/280") },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Caption boleh dikosongkan dan dapat diedit lagi dari detail foto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Button(
                        onClick = { onConfirm(drafts) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tambahkan ${drafts.size} foto")
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryViewerDialog(
    memory: Memory,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onSaveCaption: (String) -> Unit,
    onExport: (() -> Unit)? = null,
) {
    var editing by rememberSaveable(memory.id) { mutableStateOf(false) }
    var caption by rememberSaveable(memory.id) { mutableStateOf(memory.caption.orEmpty()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = HushkeepNight,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Tutup detail foto",
                            tint = HushkeepPaper
                        )
                    }
                    Text(
                        "Detail foto",
                        color = HushkeepPaper,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onFavorite) {
                        Icon(
                            if (memory.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (memory.isFavorite) "Hapus dari favorit" else "Simpan ke favorit",
                            tint = if (memory.isFavorite) HushkeepCoralLight else HushkeepPaper,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (memory.displayUri() != null) {
                        AsyncImage(
                            model = memory.displayUri(),
                            contentDescription = caption.ifBlank { "Foto kenangan" },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = HushkeepPaper)
                            Spacer(Modifier.height(12.dp))
                            Text("Foto belum tersedia", color = HushkeepPaper)
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    shape = HushkeepSheetShape,
                    color = HushkeepNightSurface,
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                    ) {
                        Text(
                            "KENANGAN",
                            style = com.ssajudn.hushkeep.ui.theme.ArchiveMetaTypography,
                            color = HushkeepCoralLight
                        )
                        Spacer(Modifier.height(6.dp))
                        if (editing) {
                            OutlinedTextField(
                                value = caption,
                                onValueChange = { caption = it.take(280) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Caption") },
                                placeholder = { Text("Tulis sesuatu yang ingin kamu ingat") },
                                minLines = 3,
                                maxLines = 5,
                                supportingText = { Text("${caption.length}/280") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = HushkeepPaper,
                                    unfocusedTextColor = HushkeepPaper,
                                    focusedLabelColor = HushkeepCoralLight,
                                    unfocusedLabelColor = HushkeepNightMuted,
                                    focusedBorderColor = HushkeepCoralLight,
                                    unfocusedBorderColor = HushkeepNightLine,
                                    cursorColor = HushkeepCoralLight,
                                    focusedPlaceholderColor = HushkeepNightMuted,
                                    unfocusedPlaceholderColor = HushkeepNightMuted,
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = HushkeepPaper),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = {
                                    caption = memory.caption.orEmpty()
                                    editing = false
                                }) { Text("Batal") }
                                Button(
                                    onClick = {
                                        onSaveCaption(caption)
                                        editing = false
                                    },
                                ) { Text("Simpan") }
                            }
                        } else {
                            Text(
                                caption.ifBlank { "Kenangan tanpa caption" },
                                style = MaterialTheme.typography.titleMedium,
                                color = HushkeepPaper,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = HushkeepNightMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    HushkeepDateTimeFormatter.timelineDay(memory.capturedAt),
                                    color = HushkeepNightMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text("•", color = HushkeepNightMuted)
                                Text(
                                    syncStatusLabel(memory.syncState),
                                    color = HushkeepNightMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = { editing = true },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Edit caption")
                                }
                                if (onExport != null) {
                                    TextButton(onClick = onExport) { Text("Download") }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = HushkeepNightLine.copy(alpha = 0.7f))
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = onDelete,
                                modifier = Modifier.fillMaxWidth(),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    HushkeepCoralLight.copy(alpha = 0.75f)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HushkeepCoralLight),
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Pindahkan ke trash")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun syncStatusLabel(syncState: SyncState): String = when (syncState) {
    SyncState.SYNCED -> "Tersimpan"
    SyncState.FAILED -> "Sinkronisasi gagal"
    SyncState.SYNCING -> "Menyinkronkan"
    SyncState.PARTIALLY_SYNCED -> "Menunggu backup"
    SyncState.PENDING -> "Menunggu backup"
    SyncState.DELETED -> "Dihapus"
}

@Composable
fun TrashScreen(
    viewModel: HushkeepViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.trash.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<TrashItem?>(null) }
    Column(Modifier.fillMaxSize()) {
        HushkeepTopBar("Trash", onBack = onBack)
        when (val currentState = state) {
            UiState.Loading -> LoadingState(Modifier.weight(1f))
            UiState.Empty -> EmptyState(
                title = "Trash kosong",
                description = "Foto yang dipindahkan ke trash akan muncul di sini.",
                modifier = Modifier.weight(1f),
            )

            is UiState.Error -> ErrorState(
                message = "Trash belum dapat dimuat.",
                modifier = Modifier.weight(1f),
            )

            is UiState.Content -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(currentState.value, key = TrashItem::id) { item ->
                    TrashRow(
                        item = item,
                        onRestore = { viewModel.restoreTrash(item) },
                        onDelete = { pendingDelete = item },
                    )
                }
            }
        }
    }

    pendingDelete?.let { item ->
        ConfirmDestructiveDialog(
            title = "Hapus permanen?",
            message = "Data ini tidak dapat dipulihkan setelah dihapus permanen.",
            confirmLabel = "Hapus permanen",
            onConfirm = {
                viewModel.permanentlyDelete(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun TrashRow(
    item: TrashItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (item.resourceType == TrashResourceType.MEMORY) "Foto kenangan" else "Album pribadi",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Dihapus dari vault",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onRestore) { Text("Pulihkan") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Hapus permanen")
            }
        }
    }
}
