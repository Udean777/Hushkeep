package com.ssajudn.hushkeep

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssajudn.hushkeep.domain.repository.AuthState
import com.ssajudn.hushkeep.feature.AppMessage
import com.ssajudn.hushkeep.feature.HushkeepViewModel
import com.ssajudn.hushkeep.feature.HushkeepViewModelFactory
import com.ssajudn.hushkeep.feature.auth.AuthLoadingScreen
import com.ssajudn.hushkeep.data.local.ThemePreferenceStore
import com.ssajudn.hushkeep.navigation.AuthNavGraph
import com.ssajudn.hushkeep.navigation.MainNavGraph
import com.ssajudn.hushkeep.ui.theme.HushkeepTheme
import com.ssajudn.hushkeep.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun HushkeepApp() {
    val context = LocalContext.current
    val container = if (LocalInspectionMode.current) null else {
        (context.applicationContext as HushkeepApplication).appContainer
    }
    if (container == null) return

    val viewModel: HushkeepViewModel = viewModel(factory = HushkeepViewModelFactory(container))
    val systemDarkTheme = isSystemInDarkTheme()
    val themeStore = remember(context) { ThemePreferenceStore(context) }
    val themeScope = rememberCoroutineScope()
    val themeMode by themeStore.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val snackbarHostState = SnackbarHostState()

    HushkeepTheme(darkTheme = isDarkTheme) {
        LaunchedEffect(viewModel) {
            viewModel.messages.collect { message ->
                when (message) {
                    is AppMessage.Text -> snackbarHostState.showSnackbar(message.value)
                }
            }
        }
        Surface {
            when (authState) {
                is AuthState.SignedIn -> MainNavGraph(
                    viewModel = viewModel,
                    themeMode = themeMode,
                    onThemeChanged = { mode -> themeScope.launch { themeStore.setThemeMode(mode) } },
                    snackbarHostState = snackbarHostState,
                )
                AuthState.Loading -> AuthLoadingScreen()
                else -> AuthNavGraph(
                    authState = authState,
                    localPreviewEnabled = viewModel.localPreviewEnabled,
                    onSignIn = viewModel::signIn,
                    onSignUp = viewModel::signUp,
                )
            }
        }
    }
}
