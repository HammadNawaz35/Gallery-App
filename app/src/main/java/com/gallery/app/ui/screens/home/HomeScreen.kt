package com.gallery.app.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gallery.app.data.models.GridSize
import com.gallery.app.data.models.MediaItem
import com.gallery.app.data.models.SortOrder
import com.gallery.app.ui.components.*
import com.gallery.app.ui.navigation.Screen
import com.gallery.app.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showSortMenu by remember { mutableStateOf(false) }
    var showGridMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedContent(
                targetState = isSelectionMode,
                label       = "topbar",
            ) { selectionMode ->
                if (selectionMode) {
                    SelectionToolbar(
                        selectedCount    = selectedIds.size,
                        onShare          = { /* share intent */ },
                        onDelete         = { viewModel.onDeleteSelected() },
                        onFavorite       = { viewModel.onFavoriteSelected() },
                        onHide           = { viewModel.onHideSelected() },
                        onClearSelection = { viewModel.clearSelection() },
                    )
                } else {
                    TopAppBar(
                        title          = { Text("Gallery", style = MaterialTheme.typography.headlineMedium) },
                        scrollBehavior = scrollBehavior,
                        actions = {
                            // Grid size picker
                            IconButton(onClick = { showGridMenu = true }) {
                                Icon(Icons.Filled.GridView, "Grid size")
                            }
                            DropdownMenu(
                                expanded        = showGridMenu,
                                onDismissRequest = { showGridMenu = false },
                            ) {
                                GridSize.values().forEach { size ->
                                    DropdownMenuItem(
                                        text    = { Text("${size.columns} columns") },
                                        onClick = { viewModel.setGridSize(size); showGridMenu = false },
                                        leadingIcon = {
                                            if (prefs.gridSize == size)
                                                Icon(Icons.Filled.Check, null)
                                        },
                                    )
                                }
                            }

                            // Sort picker
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Filled.Sort, "Sort")
                            }
                            DropdownMenu(
                                expanded        = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                            ) {
                                SortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text    = { Text(order.label()) },
                                        onClick = { viewModel.setSortOrder(order); showSortMenu = false },
                                        leadingIcon = {
                                            if (prefs.sortOrder == order)
                                                Icon(Icons.Filled.Check, null)
                                        },
                                    )
                                }
                            }

                            IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                                Icon(Icons.Filled.Search, "Search")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                    )
                }
            }
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingOverlay(Modifier.padding(paddingValues))

            uiState.mediaItems.isEmpty() -> EmptyState(
                icon     = Icons.Filled.PhotoLibrary,
                message  = "No photos yet",
                subtitle = "Photos and videos you take will appear here",
                modifier = Modifier.padding(paddingValues),
            )

            else -> MediaGrid(
                grouped      = uiState.groupedMedia,
                columns      = prefs.gridSize.columns,
                selectedIds  = selectedIds,
                onTap        = { item ->
                    if (isSelectionMode) viewModel.toggleSelection(item.id)
                    else {
                        if (item.isVideo) navController.navigate(Screen.VideoPlayer.createRoute(item.id))
                        else navController.navigate(Screen.PhotoViewer.createRoute(item.id, "all"))
                    }
                },
                onLongPress  = { item -> viewModel.toggleSelection(item.id) },
                contentPadding = paddingValues,
            )
        }
    }
}

// ── Media grid with date sections ────────────────────────

@Composable
private fun MediaGrid(
    grouped: Map<String, List<MediaItem>>,
    columns: Int,
    selectedIds: Set<Long>,
    onTap: (MediaItem) -> Unit,
    onLongPress: (MediaItem) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyVerticalGrid(
        columns             = GridCells.Fixed(columns),
        contentPadding      = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement   = Arrangement.spacedBy(2.dp),
        modifier            = Modifier.fillMaxSize(),
    ) {
        grouped.forEach { (dateLabel, items) ->
            // Section header spans all columns
            item(span = { GridItemSpan(maxLineSpan) }) {
                DateSectionHeader(label = dateLabel)
            }
            items(items = items, key = { it.id }) { item ->
                MediaGridItem(
                    item        = item,
                    isSelected  = item.id in selectedIds,
                    onTap       = { onTap(item) },
                    onLongPress = { onLongPress(item) },
                )
            }
        }
    }
}

// ── Helper extension ──────────────────────────────────────

private fun SortOrder.label(): String = when (this) {
    SortOrder.NEWEST    -> "Newest first"
    SortOrder.OLDEST    -> "Oldest first"
    SortOrder.NAME_ASC  -> "Name A–Z"
    SortOrder.NAME_DESC -> "Name Z–A"
    SortOrder.SIZE_DESC -> "Largest first"
    SortOrder.SIZE_ASC  -> "Smallest first"
}
