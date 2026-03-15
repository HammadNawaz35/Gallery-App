package com.gallery.app.data.repository

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.gallery.app.data.datasource.MediaStoreDataSource
import com.gallery.app.data.datasource.PreferencesDataSource
import com.gallery.app.data.local.dao.*
import com.gallery.app.data.local.entities.*
import com.gallery.app.data.models.*
import com.gallery.app.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStore: MediaStoreDataSource,
    private val preferences: PreferencesDataSource,
    private val favoriteDao: FavoriteDao,
    private val hiddenDao: HiddenMediaDao,
    private val trashDao: TrashDao,
) : MediaRepository {

    private val contentResolver: ContentResolver get() = context.contentResolver

    // ── Helper flows ─────────────────────────────────────────

    private val hiddenIdsFlow: Flow<Set<Long>> =
        hiddenDao.getAllHiddenIds().map { it.toSet() }

    private val trashedIdsFlow: Flow<Set<Long>> =
        trashDao.getAllTrashedIds().map { it.toSet() }

    private val favoriteIdsFlow: Flow<Set<Long>> =
        favoriteDao.getAllFavoriteIds().map { it.toSet() }

    // ── Gallery ──────────────────────────────────────────────

    override fun observeAllMedia(sortOrder: SortOrder): Flow<List<MediaItem>> =
        combine(hiddenIdsFlow, trashedIdsFlow, favoriteIdsFlow) { hidden, trashed, favs ->
            Triple(hidden, trashed, favs)
        }.flatMapLatest { (hidden, trashed, favs) ->
            flow {
                val items = mediaStore.loadAllMedia(sortOrder, hidden, trashed)
                    .map { it.copy(isFavorite = it.id in favs) }
                emit(items)
            }
        }

    override fun observeAlbums(): Flow<List<Album>> =
        combine(hiddenIdsFlow, trashedIdsFlow) { hidden, trashed -> Pair(hidden, trashed) }
            .flatMapLatest { (hidden, trashed) ->
                flow { emit(mediaStore.loadAlbums(hidden, trashed)) }
            }

    override fun observeAlbumMedia(bucketId: Long, sortOrder: SortOrder): Flow<List<MediaItem>> =
        combine(hiddenIdsFlow, trashedIdsFlow, favoriteIdsFlow) { hidden, trashed, favs ->
            Triple(hidden, trashed, favs)
        }.flatMapLatest { (hidden, trashed, favs) ->
            flow {
                val items = mediaStore.loadAlbumMedia(bucketId, sortOrder, hidden, trashed)
                    .map { it.copy(isFavorite = it.id in favs) }
                emit(items)
            }
        }

    override suspend fun getMediaById(id: Long): MediaItem? {
        val items = mediaStore.loadMediaByIds(listOf(id))
        return items.firstOrNull()
    }

    // ── Favorites ────────────────────────────────────────────

    override fun observeFavoriteIds(): Flow<Set<Long>> = favoriteIdsFlow

    override fun observeFavorites(sortOrder: SortOrder): Flow<List<MediaItem>> =
        combine(favoriteIdsFlow, hiddenIdsFlow, trashedIdsFlow) { favs, hidden, trashed ->
            Triple(favs, hidden, trashed)
        }.flatMapLatest { (favIds, hidden, trashed) ->
            flow {
                val all = mediaStore.loadAllMedia(sortOrder, hidden, trashed)
                emit(all.filter { it.id in favIds }.map { it.copy(isFavorite = true) })
            }
        }

    override suspend fun toggleFavorite(mediaId: Long) {
        val isFav = favoriteDao.isFavorite(mediaId).first()
        if (isFav) favoriteDao.removeFavorite(mediaId)
        else favoriteDao.addFavorite(FavoriteEntity(mediaId))
    }

    override suspend fun setFavorites(mediaIds: List<Long>, favorite: Boolean) {
        if (favorite) mediaIds.forEach { favoriteDao.addFavorite(FavoriteEntity(it)) }
        else favoriteDao.removeFavorites(mediaIds)
    }

    // ── Hidden / Vault ────────────────────────────────────────

    override fun observeHiddenIds(): Flow<Set<Long>> = hiddenIdsFlow

    override fun observeHiddenMedia(): Flow<List<MediaItem>> =
        hiddenDao.getHiddenMedia().flatMapLatest { entities ->
            val ids = entities.map { it.mediaId }
            flow {
                if (ids.isEmpty()) {
                    emit(emptyList())
                } else {
                    val hiddenItems = mediaStore.loadMediaByIds(ids).map { it.copy(isHidden = true) }
                    emit(hiddenItems)
                }
            }
        }

    override suspend fun hideMedia(mediaIds: List<Long>) {
        val items = mediaStore.loadMediaByIds(mediaIds)
        items.forEach { item ->
            hiddenDao.hideMedia(HiddenMediaEntity(item.id, item.path))
        }
    }

    override suspend fun unhideMedia(mediaIds: List<Long>) {
        mediaIds.forEach { hiddenDao.unhideMedia(it) }
    }

    // ── Recycle Bin ──────────────────────────────────────────

    override fun observeTrashedMedia(): Flow<List<MediaItem>> =
        trashDao.getTrashedMedia().flatMapLatest { entities ->
            val ids = entities.map { it.mediaId }
            flow {
                if (ids.isEmpty()) {
                    emit(emptyList())
                } else {
                    val trashedItems = mediaStore.loadMediaByIds(ids).map { it.copy(isTrashed = true) }
                    emit(trashedItems)
                }
            }
        }

    override suspend fun moveToTrash(mediaIds: List<Long>) {
        val items = mediaStore.loadMediaByIds(mediaIds)
        items.forEach { item ->
            trashDao.trashMedia(TrashEntity(item.id, item.path))
        }
    }

    override suspend fun restoreFromTrash(mediaIds: List<Long>) {
        mediaIds.forEach { trashDao.restoreMedia(it) }
    }

    override suspend fun permanentlyDelete(mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        mediaIds.forEach { id ->
            val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            
            // Delete from device storage
            try {
                contentResolver.delete(imageUri, null, null)
            } catch (e: Exception) {
                try {
                    contentResolver.delete(videoUri, null, null)
                } catch (e2: Exception) {
                    // Log error or handle as needed
                }
            }
            
            // Also delete from local database record
            trashDao.deleteFromTrash(listOf(id))
        }
    }

    override suspend fun purgeExpiredTrash() = trashDao.purgeExpiredTrash()

    // ── Search ───────────────────────────────────────────────

    override suspend fun search(query: SearchQuery): List<MediaItem> = withContext(Dispatchers.IO) {
        val hiddenIds = hiddenIdsFlow.first()
        val trashedIds = trashedIdsFlow.first()
        val favIds = favoriteIdsFlow.first()
        var results = mediaStore.loadAllMedia(hiddenIds = hiddenIds, trashedIds = trashedIds)
            .map { it.copy(isFavorite = it.id in favIds) }

        if (query.text.isNotBlank()) {
            results = results.filter {
                it.displayName.contains(query.text, ignoreCase = true) ||
                    it.bucketName.contains(query.text, ignoreCase = true)
            }
        }
        results = when (query.mediaFilter) {
            MediaFilter.IMAGES    -> results.filter { it.isImage }
            MediaFilter.VIDEOS    -> results.filter { it.isVideo }
            MediaFilter.FAVORITES -> results.filter { it.isFavorite }
            else -> results
        }
        query.dateRange?.let { (from, to) ->
            results = results.filter { it.dateTaken ?: it.dateAdded in from..to }
        }
        query.minSize?.let { min -> results = results.filter { it.size >= min } }
        query.maxSize?.let { max -> results = results.filter { it.size <= max } }
        results
    }

    // ── Metadata ─────────────────────────────────────────────

    override suspend fun getMetadata(mediaId: Long): MediaMetadata? = withContext(Dispatchers.IO) {
        val item = getMediaById(mediaId) ?: return@withContext null
        var make: String? = null
        var model: String? = null
        var aperture: String? = null
        var shutter: String? = null
        var iso: String? = null
        var focal: String? = null
        var lat: Double? = item.latitude
        var lon: Double? = item.longitude
        var orientation: Int? = null
        var colorSpace: String? = null

        if (item.isImage) {
            try {
                context.contentResolver.openInputStream(item.uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    make        = exif.getAttribute(ExifInterface.TAG_MAKE)
                    model       = exif.getAttribute(ExifInterface.TAG_MODEL)
                    aperture    = exif.getAttribute(ExifInterface.TAG_APERTURE_VALUE)
                    shutter     = exif.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE)
                    iso         = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                    focal       = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    colorSpace  = exif.getAttribute(ExifInterface.TAG_COLOR_SPACE)
                    val latLon = exif.latLong
                    if (latLon != null) { lat = latLon[0]; lon = latLon[1] }
                }
            } catch (_: Exception) {}
        }

        MediaMetadata(
            mediaId      = mediaId,
            resolution   = "${item.width} × ${item.height}",
            fileSize     = formatFileSize(item.size),
            mimeType     = item.mimeType,
            dateTaken    = item.dateTaken?.let { formatDate(it) },
            cameraMake   = make,
            cameraModel  = model,
            aperture     = aperture?.let { "f/$it" },
            shutterSpeed = shutter,
            iso          = iso?.let { "ISO $it" },
            focalLength  = focal?.let { "${it}mm" },
            gpsLatitude  = lat,
            gpsLongitude = lon,
            locationName = null,
            duration     = item.duration?.let { formatDuration(it) },
            width        = item.width,
            height       = item.height,
            orientation  = orientation,
            colorSpace   = colorSpace,
        )
    }

    // ── Duplicates ───────────────────────────────────────────

    override suspend fun findDuplicates(): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val all = mediaStore.loadAllMedia()
        // Group by file size first (fast pre-filter), then by content hash
        all.groupBy { it.size }
            .filter { (_, items) -> items.size > 1 }
            .flatMap { (_, items) ->
                items.groupBy { item ->
                    try {
                        context.contentResolver.openInputStream(item.uri)?.use { stream ->
                            val bytes = stream.readBytes().take(8192).toByteArray()
                            MessageDigest.getInstance("MD5")
                                .digest(bytes)
                                .joinToString("") { "%02x".format(it) }
                        } ?: "unknown_${item.id}"
                    } catch (_: Exception) { "unknown_${item.id}" }
                }.filter { (_, dupes) -> dupes.size > 1 }
                    .map { (hash, dupes) ->
                        DuplicateGroup(
                            hash = hash,
                            items = dupes,
                            wastedBytes = dupes.drop(1).sumOf { it.size },
                        )
                    }
            }
    }

    // ── Preferences ──────────────────────────────────────────

    override fun observePreferences(): Flow<AppPreferences> = preferences.preferences

    override suspend fun updatePreferences(update: (AppPreferences) -> AppPreferences) {
        val current = preferences.preferences.first()
        val updated = update(current)
        if (updated.darkMode != current.darkMode)         preferences.setDarkMode(updated.darkMode)
        if (updated.dynamicColor != current.dynamicColor) preferences.setDynamicColor(updated.dynamicColor)
        if (updated.gridSize != current.gridSize)         preferences.setGridSize(updated.gridSize)
        if (updated.sortOrder != current.sortOrder)       preferences.setSortOrder(updated.sortOrder)
        if (updated.slideshowSpeed != current.slideshowSpeed) preferences.setSlideshowSpeed(updated.slideshowSpeed)
        if (updated.vaultPin != current.vaultPin)         preferences.setVaultPin(updated.vaultPin)
        if (updated.vaultAuthType != current.vaultAuthType) preferences.setVaultAuthType(updated.vaultAuthType)
    }

    // ── Formatting helpers ───────────────────────────────────

    private fun formatFileSize(bytes: Long): String = when {
        bytes < 1024        -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        bytes < 1024L * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
    }

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours   = minutes / 60
        return if (hours > 0)
            "%d:%02d:%02d".format(hours, minutes % 60, seconds % 60)
        else
            "%d:%02d".format(minutes, seconds % 60)
    }

    private fun formatDate(epochMs: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy  HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(epochMs))
    }
}
