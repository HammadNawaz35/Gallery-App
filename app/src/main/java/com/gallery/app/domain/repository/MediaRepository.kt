package com.gallery.app.domain.repository

import com.gallery.app.data.models.*
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for all media operations.
 * The data layer provides the concrete implementation.
 */
interface MediaRepository {

    // ── Gallery ──────────────────────────────────────────────

    /** Flow of all non-hidden, non-trashed media items. */
    fun observeAllMedia(sortOrder: SortOrder): Flow<List<MediaItem>>

    /** Load albums grouped by storage bucket. */
    fun observeAlbums(): Flow<List<Album>>

    /** Load media in a specific album. */
    fun observeAlbumMedia(bucketId: Long, sortOrder: SortOrder): Flow<List<MediaItem>>

    /** Get a single media item by ID. */
    suspend fun getMediaById(id: Long): MediaItem?

    // ── Favorites ────────────────────────────────────────────

    fun observeFavoriteIds(): Flow<Set<Long>>
    fun observeFavorites(sortOrder: SortOrder): Flow<List<MediaItem>>
    suspend fun toggleFavorite(mediaId: Long)
    suspend fun setFavorites(mediaIds: List<Long>, favorite: Boolean)

    // ── Hidden / Vault ────────────────────────────────────────

    fun observeHiddenMedia(): Flow<List<MediaItem>>
    fun observeHiddenIds(): Flow<Set<Long>>
    suspend fun hideMedia(mediaIds: List<Long>)
    suspend fun unhideMedia(mediaIds: List<Long>)

    // ── Recycle Bin ──────────────────────────────────────────

    fun observeTrashedMedia(): Flow<List<MediaItem>>
    suspend fun moveToTrash(mediaIds: List<Long>)
    suspend fun restoreFromTrash(mediaIds: List<Long>)
    suspend fun permanentlyDelete(mediaIds: List<Long>)
    suspend fun purgeExpiredTrash()

    // ── Search ───────────────────────────────────────────────

    suspend fun search(query: SearchQuery): List<MediaItem>

    // ── Metadata ─────────────────────────────────────────────

    suspend fun getMetadata(mediaId: Long): MediaMetadata?

    // ── Duplicates ───────────────────────────────────────────

    suspend fun findDuplicates(): List<DuplicateGroup>

    // ── Preferences ──────────────────────────────────────────

    fun observePreferences(): Flow<AppPreferences>
    suspend fun updatePreferences(update: (AppPreferences) -> AppPreferences)
}
