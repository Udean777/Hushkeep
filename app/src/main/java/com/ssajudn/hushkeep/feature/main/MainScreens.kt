package com.ssajudn.hushkeep.feature.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ssajudn.hushkeep.core.common.UiState
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
import com.ssajudn.hushkeep.domain.model.TrashItem
import com.ssajudn.hushkeep.domain.model.TrashResourceType
import com.ssajudn.hushkeep.feature.HushkeepViewModel
import com.ssajudn.hushkeep.ui.theme.HushkeepNight
import com.ssajudn.hushkeep.ui.theme.HushkeepPaper
import com.ssajudn.hushkeep.ui.theme.HushkeepPillShape
import java.time.Instant
import java.time.ZoneId

@Composable
fun TimelineScreen(viewModel: HushkeepViewModel) {
    var selectedMemory by remember { mutableStateOf<Memory?>(null) }
    var exportTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    val timelineState by viewModel.timeline.collectAsStateWithLifecycle()
    val uploadJobs by viewModel.uploadJobs.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) viewModel.importPhotos(uris)
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

@Composable
fun SearchScreen(viewModel: HushkeepViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    var fromDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var toDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateFilter by remember { mutableStateOf<DateFilter?>(null) }
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

    Column(Modifier.fillMaxSize()) {
        HushkeepTopBar("Search")
        ArchiveHeader(
            eyebrow = "YOUR ARCHIVE",
            title = "Find a moment",
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            shape = HushkeepPillShape,
            singleLine = true,
            placeholder = { Text("Cari caption atau kata kunci") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        val albums = (albumsState as? UiState.Content)?.value.orEmpty()
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = selectedAlbumId == null,
                    onClick = { selectedAlbumId = null },
                    label = { Text("Semua album") },
                    colors = archiveFilterColors(),
                )
            }
            items(albums, key = Album::id) { album ->
                FilterChip(
                    selected = selectedAlbumId == album.id,
                    onClick = { selectedAlbumId = album.id },
                    label = { Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = archiveFilterColors(),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = { dateFilter = DateFilter.FROM },
                modifier = Modifier.weight(1f),
            ) {
                Text(fromDateMillis?.let(::formatFilterDate) ?: "Dari tanggal")
            }
            TextButton(
                onClick = { dateFilter = DateFilter.TO },
                modifier = Modifier.weight(1f),
            ) {
                Text(toDateMillis?.let(::formatFilterDate) ?: "Sampai tanggal")
            }
            if (fromDateMillis != null || toDateMillis != null || selectedAlbumId != null) {
                TextButton(
                    onClick = {
                        fromDateMillis = null
                        toDateMillis = null
                        selectedAlbumId = null
                    },
                ) { Text("Bersihkan") }
            }
        }
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
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) viewModel.importPhotos(uris, albumId)
    }
    val exportLauncher = rememberLauncherForActivityResult(CreateDocument("application/zip")) { uri ->
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
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Pindahkan album ke trash")
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
            Box(Modifier.fillMaxSize()) {
                if (memory.displayUri() != null) {
                    AsyncImage(
                        model = memory.displayUri(),
                        contentDescription = memory.caption ?: "Foto kenangan",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = HushkeepPaper)
                        Spacer(Modifier.height(12.dp))
                        Text("Foto belum tersedia", color = HushkeepPaper)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = HushkeepPaper)
                    }
                    IconButton(onClick = onFavorite) {
                        Icon(
                            if (memory.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (memory.isFavorite) "Hapus dari favorit" else "Simpan ke favorit",
                            tint = if (memory.isFavorite) MaterialTheme.colorScheme.secondary else HushkeepPaper,
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = HushkeepNight.copy(alpha = 0.88f),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        if (editing) {
                            OutlinedTextField(
                                value = caption,
                                onValueChange = { caption = it.take(280) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Caption singkat", color = HushkeepPaper) },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = HushkeepPaper),
                            )
                            Text(
                                "${caption.length}/280",
                                modifier = Modifier.align(Alignment.End),
                                color = HushkeepPaper.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { editing = false }) { Text("Batal") }
                                TextButton(onClick = {
                                    onSaveCaption(caption)
                                    editing = false
                                }) { Text("Simpan") }
                            }
                        } else {
                            Text(
                                memory.caption ?: "Kenangan tanpa caption",
                                style = MaterialTheme.typography.titleMedium,
                                color = HushkeepPaper,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(12.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End,
                            ) {
                                TextButton(onClick = { editing = true }) { Text("Edit caption") }
                                if (onExport != null) TextButton(onClick = onExport) { Text("Download") }
                                TextButton(onClick = onDelete) { Text("Pindahkan ke trash") }
                            }
                        }
                    }
                }
            }
        }
    }
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
            modifier = Modifier.fillMaxWidth().padding(14.dp),
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
