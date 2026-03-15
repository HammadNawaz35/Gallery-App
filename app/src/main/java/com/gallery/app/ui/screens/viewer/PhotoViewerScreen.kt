package com.gallery.app.ui.screens.viewer

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.app.data.models.MediaItem
import com.gallery.app.data.models.SortOrder
import com.gallery.app.domain.usecases.*
import com.gallery.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

// ── ViewModel ─────────────────────────────────────────────

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    private val getAllMedia: GetAllMediaUseCase,
    private val getAlbumMedia: GetAlbumMediaUseCase,
    private val getHiddenMedia: GetHiddenMediaUseCase,
    private val getFavorites: GetFavoritesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val hideMedia: HideMediaUseCase,
    private val getPreferences: GetPreferencesUseCase,
) : ViewModel() {

    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList.asStateFlow()

    fun loadMedia(source: String, sortOrder: SortOrder = SortOrder.NEWEST) = viewModelScope.launch {
        val flow = when {
            source == "all" -> getAllMedia(sortOrder)
            source == "favorites" -> getFavorites(sortOrder)
            source == "hidden" -> getHiddenMedia()
            source.startsWith("album_") -> {
                val bucketId = source.removePrefix("album_").toLongOrNull() ?: return@launch
                getAlbumMedia(bucketId, sortOrder)
            }
            else -> getAllMedia(sortOrder)
        }
        
        flow.map { list -> list.filter { !it.isVideo } }
            .collect { _mediaList.value = it }
    }

    fun onFavorite(id: Long) = viewModelScope.launch { toggleFavorite(id) }
    fun onDelete(id: Long)   = viewModelScope.launch { moveToTrash(listOf(id)) }
    fun onHide(id: Long)     = viewModelScope.launch { hideMedia(listOf(id)) }
}

// ── Screen ────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    navController: NavController,
    initialMediaId: Long,
    source: String,
    viewModel: PhotoViewerViewModel = hiltViewModel(),
) {
    LaunchedEffect(source) {
        viewModel.loadMedia(source)
    }

    val mediaList by viewModel.mediaList.collectAsStateWithLifecycle()
    
    if (mediaList.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startIndex = remember(mediaList, initialMediaId) {
        val index = mediaList.indexOfFirst { it.id == initialMediaId }
        if (index >= 0) index else 0
    }

    val pagerState = rememberPagerState(initialPage = startIndex) { mediaList.size }
    val currentItem = mediaList.getOrNull(pagerState.currentPage) ?: return

    var showBars by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize(),
            key      = { page -> mediaList.getOrNull(page)?.id ?: page },
            pageSpacing = 16.dp,
            beyondBoundsPageCount = 1
        ) { page ->
            val item = mediaList.getOrNull(page) ?: return@HorizontalPager
            ZoomableImage(
                uri         = item.uri,
                contentDesc = item.displayName,
                onTap       = { showBars = !showBars },
                onDismiss   = { navController.popBackStack() },
            )
        }

        AnimatedVisibility(
            visible = showBars,
            enter   = slideInVertically() + fadeIn(),
            exit    = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.6f), Color.Transparent)))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    val ctx = LocalContext.current
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = currentItem.mimeType
                            putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                        }
                        ctx.startActivity(Intent.createChooser(shareIntent, "Share"))
                    }) {
                        Icon(Icons.Filled.Share, "Share", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.onHide(currentItem.id); navController.popBackStack() }) {
                        Icon(Icons.Filled.VisibilityOff, "Hide", tint = Color.White)
                    }
                    IconButton(onClick = { navController.navigate(Screen.Editor.createRoute(currentItem.id)) }) {
                        Icon(Icons.Filled.Edit, "Edit", tint = Color.White)
                    }
                    IconButton(onClick = { navController.navigate(Screen.MediaInfo.createRoute(currentItem.id)) }) {
                        Icon(Icons.Filled.Info, "Info", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.onDelete(currentItem.id); navController.popBackStack() }) {
                        Icon(Icons.Filled.Delete, "Delete", tint = Color.White)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showBars,
            enter   = slideInVertically { it } + fadeIn(),
            exit    = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.6f))))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { viewModel.onFavorite(currentItem.id) }) {
                        Icon(
                            imageVector = if (currentItem.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentItem.isFavorite) Color(0xFFFF4081) else Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(text = "${pagerState.currentPage + 1} / ${mediaList.size}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { /* set wallpaper */ }) {
                        Icon(Icons.Filled.Wallpaper, "Set wallpaper", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    uri: android.net.Uri,
    contentDesc: String,
    onTap: () -> Unit,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }
    var dismissOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset(0f, 0f)
                        } else {
                            scale = 3f
                        }
                    }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(uri).build(),
            contentDescription = contentDesc,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y + dismissOffset
                    alpha = (1f - abs(dismissOffset) / 800f).coerceIn(0.5f, 1f)
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var isDraggingVertical = false
                        do {
                            val event = awaitPointerEvent()
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (scale > 1.05f || (event.changes.size > 1 && zoomChange != 1f)) {
                                val newScale = (scale * zoomChange).coerceIn(1f, 8f)
                                scale = newScale
                                offset += panChange
                                if (scale > 1.05f) event.changes.forEach { it.consume() }
                            } else if (scale <= 1.05f && event.changes.size == 1) {
                                if (isDraggingVertical || (abs(panChange.y) > abs(panChange.x) * 2 && abs(panChange.y) > 8)) {
                                    isDraggingVertical = true
                                    dismissOffset += panChange.y
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        if (isDraggingVertical) {
                            if (abs(dismissOffset) > 400) onDismiss() else dismissOffset = 0f
                        }
                    }
                }
        )
    }
}
