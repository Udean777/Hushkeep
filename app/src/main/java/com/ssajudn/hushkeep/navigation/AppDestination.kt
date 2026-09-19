package com.ssajudn.hushkeep.navigation

sealed class AppDestination(val route: String) {
    data object Timeline : AppDestination("timeline")
    data object Albums : AppDestination("albums")
    data object Search : AppDestination("search")
    data object Favorites : AppDestination("favorites")
    data object Settings : AppDestination("settings")
    data object Trash : AppDestination("trash")
    data object AlbumDetail : AppDestination("album/{albumId}") {
        fun createRoute(albumId: String): String = "album/$albumId"
    }
}
