package com.ssajudn.hushkeep.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssajudn.hushkeep.core.common.FileSizeFormatter
import com.ssajudn.hushkeep.core.common.UserFacingMessages
import com.ssajudn.hushkeep.domain.model.UploadJob
import com.ssajudn.hushkeep.domain.model.UploadStatus
import com.ssajudn.hushkeep.ui.theme.HushkeepSheetShape

@Composable
fun UploadProgressRow(
    jobs: List<UploadJob>,
    onRetry: (UploadJob) -> Unit,
    onRetryAll: () -> Unit,
    onCancel: (UploadJob) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = HushkeepSheetShape,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "${jobs.size} upload dalam antrean",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Status backup diperbarui saat koneksi tersedia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (jobs.any { it.status == UploadStatus.FAILED }) {
                TextButton(
                    onClick = onRetryAll,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Coba lagi semua yang gagal")
                }
            }

            jobs.take(3).forEach { job ->
                UploadJobProgressItem(
                    job = job,
                    onRetry = onRetry,
                    onCancel = onCancel,
                )
            }
        }
    }
}

@Composable
private fun UploadJobProgressItem(
    job: UploadJob,
    onRetry: (UploadJob) -> Unit,
    onCancel: (UploadJob) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = job.fileName ?: "Foto kenangan",
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (job.totalBytes > 0L) {
                Text(
                    FileSizeFormatter.format(job.totalBytes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                uploadStatusLabel(job),
                color = if (job.status == UploadStatus.FAILED) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }

        if (job.status == UploadStatus.UPLOADING) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (job.status != UploadStatus.FAILED) {
            LinearProgressIndicator(
                progress = { job.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (job.status == UploadStatus.FAILED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    UserFacingMessages.UPLOAD_FAILED,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
                TextButton(onClick = { onRetry(job) }) {
                    Text("Coba lagi")
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { onCancel(job) }) {
                    Text("Batal")
                }
            }
        }
    }
}

private fun uploadStatusLabel(job: UploadJob): String = when (job.status) {
    UploadStatus.PREPARING -> "Menyiapkan ${job.progressPercent}%"
    UploadStatus.UPLOADING -> "Mengunggah"
    UploadStatus.FAILED -> "Gagal"
    else -> "${job.progressPercent}%"
}
