package com.gallery.app.ui.screens.albums

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
import com.gallery.app.data.models.Album
import com.gallery.app.data.models.MediaItem
import com.gallery.app.data.models.SortOrder
import com.gallery.app.domain.usecases.GetAlbumsUseCase
import com.gallery.app.domain.usecases.GetAlbumMediaUseCase
import com.gallery.app.ui.components.*
import com.gallery.app.ui.navigation.Screen
import com.gallery.app.viewmodel.GalleryViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ── Albums ViewModel ──────────────────────────────────────

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val getAlbums: GetAlbumsUseCase,
) : ViewModel() {

    val albums: StateFlow<List<Album>> = getAlbums()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}

// ── AlbumsScreen ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    navController: NavController,
    viewModel: AlbumsViewModel = hiltViewModel(),
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            GalleryTopBar(title = "Albums")
        },
    ) { padding ->
        if (albums.isEmpty()) {
            EmptyState(
                icon    = Icons.Filled.PhotoAlbum,
                message = "No albums found",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyVerticalGrid(
                columns               = GridCells.Fixed(2),
                contentPadding        = PaddingValues(
                    top    = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp,
                    start  = 12.dp,
                    end    = 12.dp,
                ),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxSize(),
            ) {
                items(albums, key = { it.id }) { album ->
                    AlbumCard(
                        album   = album,
                        onClick = {
                            navController.navigate(
                                Screen.AlbumDetail.createRoute(album.id, album.name)
                            )
                        },
                    )
                }
            }
        }
    }
}

// ── AlbumDetail ViewModel ─────────────────────────────────

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val getAlbumMedia: GetAlbumMediaUseCase,
) : ViewModel() {

    private val _bucketId = MutableStateFlow<Long?>(null)

    val media: StateFlow<List<MediaItem>> = _bucketId
        .filterNotNull()
        .flatMapLatest { getAlbumMedia(it, SortOrder.NEWEST) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setBucketId(id: Long) { _bucketId.value = id }
}

// ── AlbumDetailScreen ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    navController: NavController,
    bucketId: Long,
    albumName: String,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    galleryVm: GalleryViewModel = hiltViewModel(),
) {
    LaunchedEffect(bucketId) { viewModel.setBucketId(bucketId) }

    val media by viewModel.media.collectAsStateWithLifecycle()
    val selectedIds by galleryVm.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode by galleryVm.isSelectionMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                SelectionToolbar(
                    selectedCount    = selectedIds.size,
                    onShare          = {},
                    onDelete         = { galleryVm.onDeleteSelected() },
                    onFavorite       = { galleryVm.onFavoriteSelected() },
                    onHide           = { galleryVm.onHideSelected() },
                    onClearSelection = { galleryVm.clearSelection() },
                )
            } else {
                GalleryTopBar(
                    title  = albumName,
                    onBack = { navController.popBackStack() },
                    actions = {
                        Text(
                            "${media.size} items",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        },
    ) { padding ->
        if (media.isEmpty()) {
            EmptyState(
                icon     = Icons.Filled.PhotoLibrary,
                message  = "Album is empty",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyVerticalGrid(
                columns               = GridCells.Fixed(3),
                contentPadding        = padding,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement   = Arrangement.spacedBy(2.dp),
                modifier              = Modifier.fillMaxSize(),
            ) {
                items(media, key = { it.id }) { item ->
                    MediaGridItem(
                        item        = item,
                        isSelected  = item.id in selectedIds,
                        onTap       = {
                            if (isSelectionMode) galleryVm.toggleSelection(item.id)
                            else {
                                if (item.isVideo) navController.navigate(Screen.VideoPlayer.createRoute(item.id))
                                else navController.navigate(Screen.PhotoViewer.createRoute(item.id, "album_$bucketId"))
                            }
                        },
                        onLongPress = { galleryVm.toggleSelection(item.id) },
                    )
                }
            }
        }
    }
}
