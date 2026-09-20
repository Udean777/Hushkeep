package com.ssajudn.hushkeep.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ssajudn.hushkeep.core.common.OtpPolicy
import com.ssajudn.hushkeep.ui.theme.ArchiveHeroTypography
import com.ssajudn.hushkeep.ui.theme.HushkeepPillShape
import kotlinx.coroutines.delay

@Composable
fun OtpVerificationScreen(
    email: String,
    isLoading: Boolean,
    errorMessage: String?,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
) {
    var token by rememberSaveable { mutableStateOf("") }
    var resendInSeconds by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(resendInSeconds) {
        if (resendInSeconds > 0) {
            delay(1_000)
            resendInSeconds -= 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Verifikasi email", style = ArchiveHeroTypography)
        Spacer(Modifier.height(8.dp))
        Text(
            "Masukkan kode 6 digit yang dikirim ke $email.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = OtpPolicy.normalize(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Kode verifikasi") },
            supportingText = { Text("${token.length}/${OtpPolicy.LENGTH} digit") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )
        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { onVerify(token) },
            enabled = !isLoading && OtpPolicy.isValid(token),
            modifier = Modifier.fillMaxWidth(),
            shape = HushkeepPillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Verifikasi")
            }
        }
        TextButton(
            onClick = {
                onResend()
                resendInSeconds = 60
            },
            enabled = !isLoading && resendInSeconds == 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (resendInSeconds == 0) "Kirim ulang kode" else "Kirim ulang dalam ${resendInSeconds} detik")
        }
    }
}
