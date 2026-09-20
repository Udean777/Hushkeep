package com.ssajudn.hushkeep.navigation

import androidx.navigation.NavType
import androidx.compose.runtime.Composable
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ssajudn.hushkeep.core.config.FeatureFlags
import com.ssajudn.hushkeep.domain.repository.AuthState
import com.ssajudn.hushkeep.feature.auth.AuthScreen
import com.ssajudn.hushkeep.feature.auth.OtpVerificationScreen
import com.ssajudn.hushkeep.feature.settings.PrivacyPolicyScreen

private object AuthRoute {
    const val LOGIN = "auth/login"
}

@Composable
fun AuthNavGraph(
    authState: AuthState,
    localPreviewEnabled: Boolean,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String, String) -> Unit,
    onVerifyOtp: (String, String) -> Unit,
    onResendOtp: (String) -> Unit,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AuthRoute.LOGIN) {
        composable(AuthRoute.LOGIN) {
            AuthScreen(
                authState = authState,
                localPreviewEnabled = localPreviewEnabled,
                onSignIn = onSignIn,
                onSignUp = onSignUp,
                onOpenPrivacyPolicy = { navController.navigate("auth/privacy") },
            )
        }
        composable("auth/privacy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        if (FeatureFlags.otpEnabled) {
            composable(
                route = "auth/otp?email={email}",
                arguments = listOf(navArgument("email") { type = NavType.StringType }),
            ) { entry ->
                val email = entry.arguments?.getString("email").orEmpty()
                OtpVerificationScreen(
                    email = email,
                    isLoading = authState is AuthState.Loading,
                    errorMessage = (authState as? AuthState.Error)?.message,
                    onVerify = { token -> onVerifyOtp(email, token) },
                    onResend = { onResendOtp(email) },
                )
            }
        }
    }
}
