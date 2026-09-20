package com.ssajudn.hushkeep.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssajudn.hushkeep.core.ui.components.HushkeepTopBar
import androidx.compose.foundation.BorderStroke
import com.ssajudn.hushkeep.ui.theme.HushkeepSheetShape

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        HushkeepTopBar("Privacy policy", onBack = onBack)
        Text("Hushkeep Privacy Policy", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Versi MVP 1.0 · Diperbarui 20 September 2026",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        PolicySection(
            title = "Ringkasan",
            body = "Hushkeep adalah ruang penyimpanan kenangan pribadi. Data dibuat private by default dan tidak ditampilkan dalam profil publik, feed sosial, likes, atau followers.",
        )
        PolicySection(
            title = "Data yang disimpan",
            body = "Hushkeep menyimpan email akun, username, metadata foto, nama file, ukuran dan tipe media, tanggal kenangan, caption, album, status favorit, serta status sinkronisasi. Foto yang dipilih untuk backup disimpan sebagai media pribadi.",
        )
        PolicySection(
            title = "Cara data diproses",
            body = "Data akun dan metadata diproses melalui Supabase Auth dan PostgreSQL. Media cloud disimpan di private Storage bucket Supabase. Region pemrosesan mengikuti region project Supabase yang digunakan oleh pemilik layanan.",
        )
        PolicySection(
            title = "Akses perangkat",
            body = "Hushkeep mengakses galeri hanya ketika kamu memilih foto melalui Android Photo Picker. MVP ini tidak meminta akses lokasi, kamera, mikrofon, kontak, atau penyimpanan luas di latar belakang.",
        )
        PolicySection(
            title = "Keamanan dan backup",
            body = "Akses cloud dibatasi dengan Row Level Security dan private Storage policy. Media cloud dibaca melalui signed URL atau sesi user yang sah. MVP menggunakan encryption at rest standar dari platform penyedia; end-to-end encryption belum aktif.",
        )
        PolicySection(
            title = "Export, trash, dan penghapusan",
            body = "Kamu dapat mengunduh satu foto, mengekspor album atau seluruh vault, memulihkan isi trash, dan menghapus media secara permanen. Penghapusan akun menghapus sesi, data cloud yang terkait, upload job, database lokal, dan salinan media lokal setelah proses cloud berhasil.",
        )
        PolicySection(
            title = "Retensi data",
            body = "Data aktif disimpan sampai kamu mengubah atau menghapusnya. Isi trash sederhana tidak memiliki masa pemulihan otomatis pada MVP dan tetap ada sampai dipulihkan atau dihapus permanen.",
        )
        PolicySection(
            title = "Kontak privacy",
            body = "Kontak privacy dan support resmi harus dicantumkan oleh pemilik layanan pada listing aplikasi sebelum rilis publik.",
        )
        Spacer(Modifier.padding(bottom = 12.dp))
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Surface(
        shape = HushkeepSheetShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
