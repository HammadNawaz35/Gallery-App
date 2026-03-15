package com.gallery.app.ui.screens.recycle

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
import com.gallery.app.domain.usecases.*
import com.gallery.app.ui.components.*
import com.gallery.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── RecycleBin ViewModel ──────────────────────────────────

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val getTrashedMedia: GetTrashedMediaUseCase,
    private val restore: RestoreFromTrashUseCase,
    private val permanentDelete: PermanentlyDeleteUseCase,
) : ViewModel() {

    val trashedMedia: StateFlow<List<MediaItem>> = getTrashedMedia()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> =
        selectedIds.map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleSelection(id: Long) {
        _selectedIds.update { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun restoreSelected() = viewModelScope.launch {
        restore(_selectedIds.value.toList())
        clearSelection()
    }

    fun deleteSelected() = viewModelScope.launch {
        permanentDelete(_selectedIds.value.toList())
        clearSelection()
    }

    fun deleteAll() = viewModelScope.launch {
        val all = trashedMedia.value.map { it.id }
        permanentDelete(all)
        clearSelection()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(navController: NavController, viewModel: RecycleBinViewModel = hiltViewModel()) {
    val media by viewModel.trashedMedia.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        if (isSelectionMode) {
            SelectionToolbar(
                selectedCount = selectedIds.size,
                onShare = {},
                onDelete = { viewModel.deleteSelected() },
                onFavorite = {},
                onHide = { viewModel.restoreSelected() }, 
                onClearSelection = { viewModel.clearSelection() },
                hideIcon = Icons.Filled.RestoreFromTrash,
                hideLabel = "Restore"
            )
        } else {
            GalleryTopBar(
                title  = "Recycle Bin",
                onBack = { navController.popBackStack() },
                actions = {
                    if (media.isNotEmpty()) {
                        TextButton(onClick = { showDeleteAllDialog = true }) {
                            Text("Empty", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        }
    }) { padding ->
        if (media.isEmpty()) {
            EmptyState(Icons.Filled.Delete, "Recycle bin is empty",
                "Deleted items are kept for 30 days", Modifier.padding(padding))
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
                            viewModel.toggleSelection(item.id)
                        },
                        onLongPress = { viewModel.toggleSelection(item.id) },
                    )
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title  = { Text("Empty Recycle Bin?") },
            text   = { Text("This will permanently delete all ${media.size} items. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAll(); showDeleteAllDialog = false }) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            },
        )
    }
}
