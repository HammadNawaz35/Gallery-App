package com.gallery.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.app.data.models.*
import com.gallery.app.domain.usecases.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────

data class GalleryUiState(
    val mediaItems: List<MediaItem> = emptyList(),
    val groupedMedia: Map<String, List<MediaItem>> = emptyMap(),
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

// ── ViewModel ─────────────────────────────────────────────

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModel @Inject constructor(
    private val getAllMedia: GetAllMediaUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val setFavorites: SetFavoritesUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val hideMedia: HideMediaUseCase,
    private val getPreferences: GetPreferencesUseCase,
    private val updatePreferences: UpdatePreferencesUseCase,
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = getPreferences()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())

    val uiState: StateFlow<GalleryUiState> =
        preferences.flatMapLatest { prefs ->
            getAllMedia(prefs.sortOrder).map { items ->
                GalleryUiState(
                    mediaItems   = items,
                    groupedMedia = groupByDate(items),
                    isLoading    = false,
                )
            }
        }
        .catch { e -> emit(GalleryUiState(error = e.message, isLoading = false)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GalleryUiState(isLoading = true))

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> =
        selectedIds.map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // ── Selection ─────────────────────────────────────────

    fun toggleSelection(id: Long) {
        _selectedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun selectAll() {
        _selectedIds.value = uiState.value.mediaItems.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    // ── Actions ───────────────────────────────────────────

    fun onToggleFavorite(mediaId: Long) = viewModelScope.launch {
        toggleFavorite(mediaId)
    }

    fun onFavoriteSelected() = viewModelScope.launch {
        setFavorites(_selectedIds.value.toList(), true)
        clearSelection()
    }

    fun onDeleteSelected() = viewModelScope.launch {
        moveToTrash(_selectedIds.value.toList())
        clearSelection()
    }

    fun onHideSelected() = viewModelScope.launch {
        hideMedia(_selectedIds.value.toList())
        clearSelection()
    }

    // ── Preferences ───────────────────────────────────────

    fun setSortOrder(order: SortOrder) = viewModelScope.launch {
        updatePreferences { it.copy(sortOrder = order) }
    }

    fun setGridSize(size: GridSize) = viewModelScope.launch {
        updatePreferences { it.copy(gridSize = size) }
    }

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        updatePreferences { it.copy(darkMode = enabled) }
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        updatePreferences { it.copy(dynamicColor = enabled) }
    }

    fun setSlideshowSpeed(speed: SlideshowSpeed) = viewModelScope.launch {
        updatePreferences { it.copy(slideshowSpeed = speed) }
    }

    // ── Grouping helpers ──────────────────────────────────

    private fun groupByDate(items: List<MediaItem>): Map<String, List<MediaItem>> {
        val sdf = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
        return items.groupBy { item ->
            val date = (item.dateTaken ?: (item.dateAdded * 1000))
            sdf.format(java.util.Date(date))
        }
    }
}
