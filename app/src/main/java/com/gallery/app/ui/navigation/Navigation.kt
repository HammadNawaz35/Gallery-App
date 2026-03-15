package com.gallery.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gallery.app.ui.screens.albums.AlbumsScreen
import com.gallery.app.ui.screens.albums.AlbumDetailScreen
import com.gallery.app.ui.screens.editor.EditorScreen
import com.gallery.app.ui.screens.favorites.FavoritesScreen
import com.gallery.app.ui.screens.home.HomeScreen
import com.gallery.app.ui.screens.mediainfo.MediaInfoScreen
import com.gallery.app.ui.screens.recycle.RecycleBinScreen
import com.gallery.app.ui.screens.search.SearchScreen
import com.gallery.app.ui.screens.settings.SettingsScreen
import com.gallery.app.ui.screens.vault.VaultScreen
import com.gallery.app.ui.screens.video.VideoPlayerScreen
import com.gallery.app.ui.screens.viewer.PhotoViewerScreen

// ── Route Definitions ─────────────────────────────────────

sealed class Screen(val route: String) {
    object Home        : Screen("home")
    object Albums      : Screen("albums")
    object Favorites   : Screen("favorites")
    object Search      : Screen("search")
    object Vault       : Screen("vault")
    object RecycleBin  : Screen("recycle_bin")
    object Settings    : Screen("settings")

    object AlbumDetail : Screen("album/{bucketId}/{albumName}") {
        fun createRoute(bucketId: Long, albumName: String) = "album/$bucketId/$albumName"
    }
    object PhotoViewer : Screen("viewer/{mediaId}?source={source}") {
        fun createRoute(mediaId: Long, source: String = "all") = "viewer/$mediaId?source=$source"
    }
    object VideoPlayer : Screen("video/{mediaId}") {
        fun createRoute(mediaId: Long) = "video/$mediaId"
    }
    object MediaInfo   : Screen("media_info/{mediaId}") {
        fun createRoute(mediaId: Long) = "media_info/$mediaId"
    }
    object Editor      : Screen("editor/{mediaId}") {
        fun createRoute(mediaId: Long) = "editor/$mediaId"
    }
}

// ── Nav Graph ─────────────────────────────────────────────

@Composable
fun GalleryNavGraph(navController: NavHostController) {
    NavHost(
        navController   = navController,
        startDestination = Screen.Home.route,
        enterTransition  = { slideInHorizontally(tween(300)) { it / 2 } + fadeIn(tween(300)) },
        exitTransition   = { slideOutHorizontally(tween(300)) { -it / 2 } + fadeOut(tween(300)) },
        popEnterTransition  = { slideInHorizontally(tween(300)) { -it / 2 } + fadeIn(tween(300)) },
        popExitTransition   = { slideOutHorizontally(tween(300)) { it / 2 } + fadeOut(tween(300)) },
    ) {
        // Main tabs
        composable(Screen.Home.route)      { HomeScreen(navController) }
        composable(Screen.Albums.route)    { AlbumsScreen(navController) }
        composable(Screen.Favorites.route) { FavoritesScreen(navController) }
        composable(Screen.Search.route)    { SearchScreen(navController) }
        composable(Screen.Vault.route)     { VaultScreen(navController) }
        composable(Screen.RecycleBin.route){ RecycleBinScreen(navController) }
        composable(Screen.Settings.route)  { SettingsScreen(navController) }

        // Album detail
        composable(
            route    = Screen.AlbumDetail.route,
            arguments = listOf(
                navArgument("bucketId")   { type = NavType.LongType },
                navArgument("albumName")  { type = NavType.StringType },
            ),
        ) { entry ->
            AlbumDetailScreen(
                navController = navController,
                bucketId      = entry.arguments!!.getLong("bucketId"),
                albumName     = entry.arguments!!.getString("albumName") ?: "",
            )
        }

        // Photo viewer
        composable(
            route     = Screen.PhotoViewer.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.LongType },
                navArgument("source")  { type = NavType.StringType; defaultValue = "all" },
            ),
        ) { entry ->
            val source = entry.arguments!!.getString("source") ?: "all"
            PhotoViewerScreen(
                navController = navController,
                initialMediaId = entry.arguments!!.getLong("mediaId"),
                source         = source,
            )
        }

        // Video player
        composable(
            route     = Screen.VideoPlayer.route,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType }),
        ) { entry ->
            VideoPlayerScreen(
                navController = navController,
                mediaId       = entry.arguments!!.getLong("mediaId"),
            )
        }

        // Media info
        composable(
            route     = Screen.MediaInfo.route,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType }),
        ) { entry ->
            MediaInfoScreen(
                navController = navController,
                mediaId       = entry.arguments!!.getLong("mediaId"),
            )
        }

        // Editor
        composable(
            route     = Screen.Editor.route,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType }),
        ) { entry ->
            EditorScreen(
                navController = navController,
                mediaId       = entry.arguments!!.getLong("mediaId"),
            )
        }
    }
}
