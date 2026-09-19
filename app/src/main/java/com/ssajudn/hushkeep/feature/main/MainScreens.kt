package com.ssajudn.hushkeep.feature.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ssajudn.hushkeep.core.ui.components.ArchiveHeader
import com.ssajudn.hushkeep.core.ui.components.ConfirmDestructiveDialog
import com.ssajudn.hushkeep.core.ui.components.EmptyState
import com.ssajudn.hushkeep.core.ui.components.ErrorState
import com.ssajudn.hushkeep.core.ui.components.HushkeepTopBar
import com.ssajudn.hushkeep.core.ui.components.LoadingState
import com.ssajudn.hushkeep.core.ui.components.UploadProgressRow
import com.ssajudn.hushkeep.domain.model.Album
import com.ssajudn.hushkeep.domain.model.Memory
import com.ssajudn.hushkeep.domain.model.TrashItem
import com.ssajudn.hushkeep.domain.model.TrashResourceType
import com.ssajudn.hushkeep.feature.HushkeepViewModel
import com.ssajudn.hushkeep.ui.theme.HushkeepNight
import com.ssajudn.hushkeep.ui.theme.HushkeepPaper

@Composable
fun TimelineScreen(viewModel: HushkeepViewModel) {
    var selectedMemory by remember { mutableStateOf<Memory?>(null) }
    val timelineState by viewModel.timeline.collectAsStateWithLifecycle()
    val activeUploadCount by viewModel.activeUploadCount.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) viewModel.importPhotos(uris)
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
                visible = activeUploadCount > 0,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
            ) {
                UploadProgressRow(activeUploadCount)
            }
            MemoryListContent(
                state = timelineState,
                onFavorite = viewModel::toggleFavorite,
                onDelete = viewModel::deleteMemory,
                onOpen = { selectedMemory = it },
                modifier = Modifier.weight(1f),
            )
        }
        FloatingActionButton(
            onClick = {
                picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Keep a memory")
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
}

@Composable
fun SearchScreen(viewModel: HushkeepViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedMemory by remember { mutableStateOf<Memory?>(null) }
    val state by remember(query) { viewModel.search(query) }
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
            singleLine = true,
            placeholder = { Text("Cari caption atau kata kunci") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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
                        AlbumCard(album = album, onClick = { onOpenAlbum(album.id) })
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Buat album")
        }
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
fun AlbumDetailScreen(
    albumId: String,
    viewModel: HushkeepViewModel,
    onBack: () -> Unit,
) {
    val state by remember(albumId) { viewModel.observeAlbum(albumId) }
        .collectAsStateWithLifecycle(initialValue = UiState.Loading)
    var selectedMemory by remember { mutableStateOf<Memory?>(null) }
    Column(Modifier.fillMaxSize()) {
        HushkeepTopBar("Album", onBack = onBack)
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
private fun MemoryViewerDialog(
    memory: Memory,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onSaveCaption: (String) -> Unit,
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
                if (memory.localUri != null) {
                    AsyncImage(
                        model = Uri.parse(memory.localUri),
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
                                onValueChange = { caption = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Caption singkat", color = HushkeepPaper) },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = HushkeepPaper),
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TextButton(onClick = { editing = true }) { Text("Edit caption") }
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
