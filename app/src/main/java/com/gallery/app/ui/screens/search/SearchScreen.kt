package com.gallery.app.ui.screens.search

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.gallery.app.data.models.*
import com.gallery.app.domain.usecases.SearchMediaUseCase
import com.gallery.app.ui.components.*
import com.gallery.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMedia: SearchMediaUseCase,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(MediaFilter.ALL)
    val filter: StateFlow<MediaFilter> = _filter.asStateFlow()

    val results: StateFlow<List<MediaItem>> =
        combine(_query.debounce(300), _filter) { q, f -> Pair(q, f) }
            .flatMapLatest { (q, f) ->
                flow { emit(searchMedia(SearchQuery(text = q, mediaFilter = f))) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isSearching: StateFlow<Boolean> = _query.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onQueryChange(q: String) { _query.value = q }
    fun onFilterChange(f: MediaFilter) { _filter.value = f }
    fun clearQuery() { _query.value = "" }
}

// ── Screen ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                    GallerySearchBar(
                        query         = query,
                        onQueryChange = viewModel::onQueryChange,
                        onSearch      = viewModel::onQueryChange,
                        modifier      = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                    )
                }

                // Filter chips
                ScrollableTabRow(
                    selectedTabIndex = MediaFilter.values().indexOf(filter),
                    edgePadding      = 12.dp,
                    divider          = {},
                ) {
                    MediaFilter.values().forEach { f ->
                        Tab(
                            selected = filter == f,
                            onClick  = { viewModel.onFilterChange(f) },
                            text     = { Text(f.label()) },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        AnimatedContent(
            targetState = isSearching,
            label       = "search_state",
            modifier    = Modifier.padding(paddingValues),
        ) { searching ->
            if (!searching) {
                // Suggestions / recent
                SearchSuggestions()
            } else if (results.isEmpty()) {
                EmptyState(
                    icon    = Icons.Filled.SearchOff,
                    message = "No results for \"$query\"",
                )
            } else {
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement   = Arrangement.spacedBy(2.dp),
                    modifier              = Modifier.fillMaxSize(),
                ) {
                    items(results, key = { it.id }) { item ->
                        MediaGridItem(
                            item        = item,
                            isSelected  = false,
                            onTap       = {
                                if (item.isVideo) navController.navigate(Screen.VideoPlayer.createRoute(item.id))
                                else navController.navigate(Screen.PhotoViewer.createRoute(item.id))
                            },
                            onLongPress = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestions() {
    val suggestions = listOf(
        "Camera" to Icons.Filled.CameraAlt,
        "Screenshots" to Icons.Filled.Screenshot,
        "Videos" to Icons.Filled.VideoLibrary,
        "Favorites" to Icons.Filled.Favorite,
        "Downloads" to Icons.Filled.Download,
    )
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Quick Search",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
        items(suggestions.size) { i ->
            val (label, icon) = suggestions[i]
            ListItem(
                leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun MediaFilter.label() = when (this) {
    MediaFilter.ALL       -> "All"
    MediaFilter.IMAGES    -> "Photos"
    MediaFilter.VIDEOS    -> "Videos"
    MediaFilter.FAVORITES -> "Favorites"
    MediaFilter.HIDDEN    -> "Hidden"
    MediaFilter.TRASHED   -> "Trash"
}
