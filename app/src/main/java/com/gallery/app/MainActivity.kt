package com.gallery.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gallery.app.ui.navigation.GalleryNavGraph
import com.gallery.app.ui.navigation.Screen
import com.gallery.app.ui.theme.GalleryTheme
import com.gallery.app.viewmodel.GalleryViewModel
import com.google.accompanist.permissions.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: GalleryViewModel = hiltViewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()

            GalleryTheme(
                darkTheme    = prefs.darkMode,
                dynamicColor = prefs.dynamicColor,
            ) {
                PermissionGate {
                    MainScaffold()
                }
            }
        }
    }
}

// ── Permission Gate ───────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permState = rememberMultiplePermissionsState(permissions)

    if (permState.allPermissionsGranted) {
        content()
    } else {
        PermissionDeniedScreen(
            onRequest = { permState.launchMultiplePermissionRequest() },
            isPermanentlyDenied = permState.permissions.any { it.status.shouldShowRationale.not() && it.status is PermissionStatus.Denied },
        )
    }
}

@Composable
private fun PermissionDeniedScreen(onRequest: () -> Unit, isPermanentlyDenied: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier.padding(32.dp),
        ) {
            Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Gallery needs access to your photos",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                if (isPermanentlyDenied)
                    "Please grant access in Settings → App Permissions"
                else "Tap below to grant access to your photos and videos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text(if (isPermanentlyDenied) "Open Settings" else "Grant Access")
            }
        }
    }
}

// ── Main Scaffold with Bottom Nav ─────────────────────────

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,      "Gallery",   Icons.Filled.PhotoLibrary,   Icons.Outlined.PhotoLibrary),
    BottomNavItem(Screen.Albums,    "Albums",    Icons.Filled.PhotoAlbum,     Icons.Outlined.PhotoAlbum),
    BottomNavItem(Screen.Favorites, "Favorites", Icons.Filled.Favorite,       Icons.Outlined.FavoriteBorder),
    BottomNavItem(Screen.Settings,  "Settings",  Icons.Filled.Settings,       Icons.Outlined.Settings),
)

// Screens where bottom nav should be hidden
private val fullscreenRoutes = setOf(
    Screen.PhotoViewer.route.substringBefore("/"),
    Screen.VideoPlayer.route.substringBefore("/"),
    Screen.Editor.route.substringBefore("/"),
    Screen.MediaInfo.route.substringBefore("/"),
)

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    val showBottomBar = bottomNavItems.any { it.screen.route == currentRoute } ||
        !fullscreenRoutes.any { currentRoute.startsWith(it) }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = showBottomBar && bottomNavItems.any { it.screen.route == currentRoute }) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = navBackStackEntry?.destination?.hierarchy
                            ?.any { it.route == item.screen.route } == true

                        NavigationBarItem(
                            selected = isSelected,
                            onClick  = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = {
                                Icon(
                                    if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    item.label,
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { _ ->
        GalleryNavGraph(navController)
    }
}
