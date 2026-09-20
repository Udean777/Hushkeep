package com.ssajudn.hushkeep.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssajudn.hushkeep.core.ui.components.ArchiveNavigationBar
import com.ssajudn.hushkeep.core.ui.components.ArchiveNavigationItem
import com.ssajudn.hushkeep.core.ui.components.ArchiveNavigationRail
import com.ssajudn.hushkeep.feature.HushkeepViewModel
import com.ssajudn.hushkeep.feature.main.AlbumDetailScreen
import com.ssajudn.hushkeep.feature.main.AlbumsScreen
import com.ssajudn.hushkeep.feature.main.SearchScreen
import com.ssajudn.hushkeep.feature.main.TimelineScreen
import com.ssajudn.hushkeep.feature.main.TrashScreen
import com.ssajudn.hushkeep.feature.settings.SettingsScreen
import com.ssajudn.hushkeep.feature.settings.PrivacyPolicyScreen
import com.ssajudn.hushkeep.ui.theme.ThemeMode

private val navigationDestinations = listOf(
    ArchiveNavigationItem(AppDestination.Timeline.route, "Vault", Icons.Default.Home),
    ArchiveNavigationItem(AppDestination.Albums.route, "Albums", Icons.Default.CollectionsBookmark),
    ArchiveNavigationItem(AppDestination.Search.route, "Search", Icons.Default.Search),
    ArchiveNavigationItem(AppDestination.Settings.route, "You", Icons.Default.Person),
)

@Composable
fun MainNavGraph(
    viewModel: HushkeepViewModel,
    themeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showNavigation = navigationDestinations.any { it.route == currentRoute }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isCompact && showNavigation) {
                    ArchiveNavigationBar(
                        items = navigationDestinations,
                        currentRoute = currentRoute,
                        onNavigate = { route -> navigateTopLevel(navController, route) },
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (!isCompact && showNavigation) {
                    ArchiveNavigationRail(
                        items = navigationDestinations,
                        currentRoute = currentRoute,
                        onNavigate = { route -> navigateTopLevel(navController, route) },
                    )
                }
                NavHost(
                    navController = navController,
                    startDestination = AppDestination.Timeline.route,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(AppDestination.Timeline.route) { TimelineScreen(viewModel) }
                    composable(AppDestination.Albums.route) {
                        AlbumsScreen(viewModel) { albumId ->
                            navController.navigate(AppDestination.AlbumDetail.createRoute(albumId))
                        }
                    }
                    composable(AppDestination.Search.route) { SearchScreen(viewModel) }
                    composable(AppDestination.Settings.route) {
                        SettingsScreen(
                            viewModel = viewModel,
                            themeMode = themeMode,
                            onThemeChanged = onThemeChanged,
                            onOpenTrash = { navController.navigate(AppDestination.Trash.route) },
                            onOpenPrivacyPolicy = { navController.navigate(AppDestination.PrivacyPolicy.route) },
                        )
                    }
                    composable(AppDestination.PrivacyPolicy.route) {
                        PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                    }
                    composable(AppDestination.Trash.route) {
                        TrashScreen(viewModel) { navController.popBackStack() }
                    }
                    composable(AppDestination.AlbumDetail.route) { backStackEntry ->
                        val albumId = backStackEntry.arguments?.getString("albumId")
                            ?: return@composable
                        AlbumDetailScreen(
                            albumId = albumId,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

private fun navigateTopLevel(
    navController: NavHostController,
    route: String,
) {
    navController.navigate(route) {
        popUpTo(AppDestination.Timeline.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
