package com.ssajudn.hushkeep.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ssajudn.hushkeep.domain.repository.AuthState
import com.ssajudn.hushkeep.feature.auth.AuthScreen

private object AuthRoute {
    const val LOGIN = "auth/login"
}

@Composable
fun AuthNavGraph(
    authState: AuthState,
    localPreviewEnabled: Boolean,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String, String) -> Unit,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AuthRoute.LOGIN) {
        composable(AuthRoute.LOGIN) {
            AuthScreen(
                authState = authState,
                localPreviewEnabled = localPreviewEnabled,
                onSignIn = onSignIn,
                onSignUp = onSignUp,
            )
        }
    }
}
