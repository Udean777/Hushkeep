package com.ssajudn.hushkeep.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssajudn.hushkeep.feature.HushkeepViewModel
import com.ssajudn.hushkeep.feature.main.AlbumDetailScreen
import com.ssajudn.hushkeep.feature.main.AlbumsScreen
import com.ssajudn.hushkeep.feature.main.SearchScreen
import com.ssajudn.hushkeep.feature.main.TimelineScreen
import com.ssajudn.hushkeep.feature.main.TrashScreen
import com.ssajudn.hushkeep.feature.settings.SettingsScreen
import com.ssajudn.hushkeep.ui.theme.ThemeMode

private data class NavigationDestination(
    val destination: AppDestination,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val navigationDestinations = listOf(
    NavigationDestination(AppDestination.Timeline, "Vault", Icons.Default.Home),
    NavigationDestination(AppDestination.Albums, "Albums", Icons.Default.CollectionsBookmark),
    NavigationDestination(AppDestination.Search, "Search", Icons.Default.Search),
    NavigationDestination(AppDestination.Settings, "You", Icons.Default.Person),
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
    val showNavigation = navigationDestinations.any { it.destination.route == currentRoute }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isCompact && showNavigation) {
                    BottomNavigationBar(navController, currentRoute)
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (!isCompact && showNavigation) {
                    NavigationRail {
                        navigationDestinations.forEach { item ->
                            NavigationRailItem(
                                selected = currentRoute == item.destination.route,
                                onClick = { navigateTopLevel(navController, item.destination) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                alwaysShowLabel = true,
                            )
                        }
                    }
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
                        )
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

@Composable
private fun BottomNavigationBar(
    navController: NavHostController,
    currentRoute: String?,
) {
    NavigationBar {
        navigationDestinations.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.destination.route,
                onClick = { navigateTopLevel(navController, item.destination) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

private fun navigateTopLevel(
    navController: NavHostController,
    destination: AppDestination,
) {
    navController.navigate(destination.route) {
        popUpTo(AppDestination.Timeline.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
