package com.ssajudn.hushkeep.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssajudn.hushkeep.core.ui.components.HushkeepTopBar
import com.ssajudn.hushkeep.core.ui.components.PrivacyNotice
import com.ssajudn.hushkeep.core.ui.components.StorageUsageIndicator
import com.ssajudn.hushkeep.feature.HushkeepViewModel
import com.ssajudn.hushkeep.ui.theme.ThemeMode
import com.ssajudn.hushkeep.ui.theme.HushkeepSheetShape

@Composable
fun SettingsScreen(
    viewModel: HushkeepViewModel,
    themeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    val usage by viewModel.storageUsage.collectAsStateWithLifecycle()
    val uploadJobs by viewModel.uploadJobs.collectAsStateWithLifecycle()
    var showAccountDeletionInfo by rememberSaveable { mutableStateOf(false) }
    var accountPassword by rememberSaveable { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.exportData(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HushkeepTopBar("You")
        PrivacyNotice()
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = HushkeepSheetShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            StorageUsageIndicator(usage, modifier = Modifier.padding(18.dp))
        }
        Text(
            if (uploadJobs.isEmpty()) "Semua kenangan sudah diproses." else "${uploadJobs.size} upload masih diproses.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Tampilan", style = MaterialTheme.typography.titleMedium)
        Text(
            "Pilih cara Hushkeep tampil di perangkat ini.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = HushkeepSheetShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeChanged(mode) },
                        label = { Text(mode.label()) },
                        leadingIcon = {
                            Icon(
                                when (mode) {
                                    ThemeMode.SYSTEM -> Icons.Default.Info
                                    ThemeMode.LIGHT -> Icons.Default.WbSunny
                                    ThemeMode.DARK -> Icons.Default.Nightlight
                                },
                                contentDescription = null,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        ListItem(
            leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
            headlineContent = { Text("Privacy policy") },
            supportingContent = { Text("Cara Hushkeep memperlakukan data pribadi.") },
            trailingContent = { TextButton(onClick = onOpenPrivacyPolicy) { Text("Baca") } },
        )
        ListItem(
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            headlineContent = { Text("Export kenangan") },
            supportingContent = { Text("Simpan foto dan metadata ke file ZIP.") },
            trailingContent = {
                TextButton(onClick = { exportLauncher.launch("hushkeep-export.zip") }) {
                    Text("Export")
                }
            },
        )
        ListItem(
            leadingContent = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            headlineContent = { Text("Trash") },
            supportingContent = { Text("Pulihkan atau hapus kenangan secara permanen.") },
            trailingContent = { TextButton(onClick = onOpenTrash) { Text("Buka") } },
        )
        ListItem(
            leadingContent = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            headlineContent = { Text("Penghapusan akun") },
            supportingContent = { Text("Hapus akun dan data secara permanen.") },
            trailingContent = {
                TextButton(onClick = { showAccountDeletionInfo = true }) { Text("Hapus") }
            },
        )
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Keluar")
        }
    }

    if (showAccountDeletionInfo) {
        AlertDialog(
            onDismissRequest = { showAccountDeletionInfo = false },
            title = { Text("Penghapusan akun") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Penghapusan akun akan menghapus sesi, metadata, dan media yang terkait dengan akun ini secara permanen. Pastikan kamu sudah mengunduh export sebelum melanjutkan.",
                    )
                    OutlinedTextField(
                        value = accountPassword,
                        onValueChange = { accountPassword = it },
                        label = { Text("Password untuk konfirmasi") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAccountDeletionInfo = false
                    viewModel.deleteAccount(accountPassword)
                    accountPassword = ""
                }, enabled = accountPassword.isNotBlank()) { Text("Hapus akun") }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDeletionInfo = false }) { Text("Batal") }
            },
        )
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
