package com.gallery.app.domain.usecases

import com.gallery.app.data.models.*
import com.gallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// ── Gallery Use Cases ─────────────────────────────────────

class GetAllMediaUseCase @Inject constructor(private val repo: MediaRepository) {
    operator fun invoke(sortOrder: SortOrder): Flow<List<MediaItem>> =
        repo.observeAllMedia(sortOrder)
}

class GetAlbumsUseCase @Inject constructor(private val repo: MediaRepository) {
    operator fun invoke(): Flow<List<Album>> = repo.observeAlbums()
}

class GetAlbumMediaUseCase @Inject constructor(private val repo: MediaRepository) {
    operator fun invoke(bucketId: Long, sortOrder: SortOrder): Flow<List<MediaItem>> =
        repo.observeAlbumMedia(bucketId, sortOrder)
}

class GetMediaByIdUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(id: Long): MediaItem? = repo.getMediaById(id)
}

// ── Favorite Use Cases ────────────────────────────────────

class GetFavoritesUseCase @Inject constructor(private val repo: MediaRepository) {
    operator fun invoke(sortOrder: SortOrder): Flow<List<MediaItem>> =
        repo.observeFavorites(sortOrder)
}

class ToggleFavoriteUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(mediaId: Long) = repo.toggleFavorite(mediaId)
}

class SetFavoritesUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(mediaIds: List<Long>, favorite: Boolean) =
        repo.setFavorites(mediaIds, favorite)
}

// ── Vault Use Cases ───────────────────────────────────────

class GetHiddenMediaUseCase @Inject constructor(private val repo: MediaRepository) {
    operator fun invoke(): Flow<List<MediaItem>> = repo.observeHiddenMedia()
}

class HideMediaUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(mediaIds: List<Long>) = repo.hideMedia(mediaIds)
}

class UnhideMediaUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(mediaIds: List<Long>) = repo.unhideMedia(mediaIds)
}

// ── Trash Use Cases ───────────────────────────────────────

class GetTrashedMediaUseCase @Inject constructor(private val repo: MediaRepository) {
    operator fun invoke(): Flow<List<MediaItem>> = repo.observeTrashedMedia()
}

class MoveToTrashUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(mediaIds: List<Long>) = repo.moveToTrash(mediaIds)
}

class RestoreFromTrashUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(mediaIds: List<Long>) = repo.restoreFromTrash(mediaIds)
}

class PermanentlyDeleteUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(mediaIds: List<Long>) = repo.permanentlyDelete(mediaIds)
}

// ── Search Use Cases ──────────────────────────────────────

class SearchMediaUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(query: SearchQuery): List<MediaItem> = repo.search(query)
}

// ── Metadata Use Cases ────────────────────────────────────

class GetMetadataUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(mediaId: Long): MediaMetadata? = repo.getMetadata(mediaId)
}

// ── Duplicate Use Cases ───────────────────────────────────

class FindDuplicatesUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(): List<DuplicateGroup> = repo.findDuplicates()
}

// ── Preferences Use Cases ─────────────────────────────────

class GetPreferencesUseCase @Inject constructor(private val repo: MediaRepository) {
    operator fun invoke(): Flow<AppPreferences> = repo.observePreferences()
}

class UpdatePreferencesUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(update: (AppPreferences) -> AppPreferences) =
        repo.updatePreferences(update)
}
