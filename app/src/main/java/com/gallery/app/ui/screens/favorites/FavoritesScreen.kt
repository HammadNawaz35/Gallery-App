package com.gallery.app.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.gallery.app.data.models.MediaItem
import com.gallery.app.data.models.SortOrder
import com.gallery.app.domain.usecases.GetFavoritesUseCase
import com.gallery.app.domain.usecases.ToggleFavoriteUseCase
import com.gallery.app.ui.components.*
import com.gallery.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavorites: GetFavoritesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {
    val favorites: StateFlow<List<MediaItem>> = getFavorites(SortOrder.NEWEST)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onToggle(id: Long) = viewModelScope.launch { toggleFavorite(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController, viewModel: FavoritesViewModel = hiltViewModel()) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    Scaffold(topBar = { GalleryTopBar("Favorites") }) { padding ->
        if (favorites.isEmpty()) {
            EmptyState(Icons.Filled.FavoriteBorder, "No favorites yet",
                "Tap the heart on any photo to add it here", Modifier.padding(padding))
        } else {
            LazyVerticalGrid(
                columns               = GridCells.Fixed(3),
                contentPadding        = padding,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement   = Arrangement.spacedBy(2.dp),
                modifier              = Modifier.fillMaxSize(),
            ) {
                items(favorites, key = { it.id }) { item ->
                    MediaGridItem(
                        item        = item,
                        isSelected  = false,
                        onTap       = {
                            if (item.isVideo) navController.navigate(Screen.VideoPlayer.createRoute(item.id))
                            else navController.navigate(Screen.PhotoViewer.createRoute(item.id, "favorites"))
                        },
                        onLongPress = { viewModel.onToggle(item.id) },
                    )
                }
            }
        }
    }
}
