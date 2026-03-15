package com.gallery.app.data.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.gallery.app.data.models.Album
import com.gallery.app.data.models.MediaItem
import com.gallery.app.data.models.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataSource that queries the device MediaStore for all photos and videos.
 * Supports Android SDK 26+ with proper permission handling for SDK 33+.
 */
@Singleton
class MediaStoreDataSource @Inject constructor(
    private val contentResolver: ContentResolver,
) {

    // ──────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────

    /**
     * Load all media items from the device.
     * @param sortOrder  How to sort results.
     * @param hiddenIds  Set of media IDs to exclude (vault).
     * @param trashedIds Set of media IDs to exclude (trash).
     */
    suspend fun loadAllMedia(
        sortOrder: SortOrder = SortOrder.NEWEST,
        hiddenIds: Set<Long> = emptySet(),
        trashedIds: Set<Long> = emptySet(),
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        items += queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, sortOrder)
        items += queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, sortOrder)

        items
            .filter { it.id !in hiddenIds && it.id !in trashedIds }
            .sortedWith(sortOrder.comparator())
    }

    /**
     * Load specific media items by their IDs.
     */
    suspend fun loadMediaByIds(ids: List<Long>): List<MediaItem> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val idString = ids.joinToString(",")
        val selection = "${MediaStore.MediaColumns._ID} IN ($idString)"
        
        val items = mutableListOf<MediaItem>()
        items += queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, SortOrder.NEWEST, selection)
        items += queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, SortOrder.NEWEST, selection)
        items
    }

    /**
     * Load albums by grouping media into their storage buckets.
     */
    suspend fun loadAlbums(
        hiddenIds: Set<Long> = emptySet(),
        trashedIds: Set<Long> = emptySet(),
    ): List<Album> = withContext(Dispatchers.IO) {
        val allMedia = loadAllMedia(hiddenIds = hiddenIds, trashedIds = trashedIds)
        allMedia
            .groupBy { it.bucketId }
            .map { (bucketId, items) ->
                val first = items.maxByOrNull { it.dateModified }!!
                Album(
                    id = bucketId,
                    name = first.bucketName,
                    coverUri = first.uri,
                    mediaCount = items.size,
                    dateModified = first.dateModified,
                    path = first.path.substringBeforeLast("/"),
                )
            }
            .sortedByDescending { it.dateModified }
    }

    /**
     * Load media items belonging to a specific album (bucket).
     */
    suspend fun loadAlbumMedia(
        bucketId: Long,
        sortOrder: SortOrder = SortOrder.NEWEST,
        hiddenIds: Set<Long> = emptySet(),
        trashedIds: Set<Long> = emptySet(),
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val all = loadAllMedia(sortOrder = sortOrder, hiddenIds = hiddenIds, trashedIds = trashedIds)
        all.filter { it.bucketId == bucketId }
    }

    /**
     * Search media by filename, date, or size.
     */
    suspend fun searchMedia(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val all = loadAllMedia()
        if (query.isBlank()) all
        else all.filter {
            it.displayName.contains(query, ignoreCase = true) ||
                it.bucketName.contains(query, ignoreCase = true)
        }
    }

    fun SortOrder.comparator(): Comparator<MediaItem> = when (this) {
        SortOrder.NEWEST    -> compareByDescending { it.dateAdded }
        SortOrder.OLDEST    -> compareBy { it.dateAdded }
        SortOrder.NAME_ASC  -> compareBy { it.displayName }
        SortOrder.NAME_DESC -> compareByDescending { it.displayName }
        SortOrder.SIZE_DESC -> compareByDescending { it.size }
        SortOrder.SIZE_ASC  -> compareBy { it.size }
    }

    // ──────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────

    private fun queryMedia(
        contentUri: Uri,
        sortOrder: SortOrder,
        selection: String? = null,
    ): List<MediaItem> {
        val isVideo = contentUri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = buildProjection(isVideo)
        val orderClause = sortOrder.toSqlOrderClause()

        val cursor: Cursor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bundle = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, orderClause)
                if (selection != null) {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                }
            }
            contentResolver.query(contentUri, projection, bundle, null)
        } else {
            contentResolver.query(contentUri, projection, selection, null, orderClause)
        }

        return cursor?.use { c ->
            val items = mutableListOf<MediaItem>()
            val idCol         = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol       = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol       = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeCol       = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val addedCol      = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val modifiedCol   = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val widthCol      = c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol     = c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val bucketIdCol   = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val dataCol       = c.getColumnIndex(MediaStore.MediaColumns.DATA)
            val dateTakenCol  = c.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            val durationCol   = if (isVideo) c.getColumnIndex(MediaStore.Video.Media.DURATION) else -1
            val latCol        = c.getColumnIndex(MediaStore.Images.Media.LATITUDE)
            val lonCol        = c.getColumnIndex(MediaStore.Images.Media.LONGITUDE)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(contentUri, id)
                val path = if (dataCol >= 0) c.getString(dataCol) ?: "" else ""

                items += MediaItem(
                    id           = id,
                    uri          = uri,
                    displayName  = c.getString(nameCol) ?: "",
                    mimeType     = c.getString(mimeCol) ?: if (isVideo) "video/*" else "image/*",
                    size         = c.getLong(sizeCol),
                    dateAdded    = c.getLong(addedCol),
                    dateModified = c.getLong(modifiedCol),
                    dateTaken    = if (dateTakenCol >= 0) c.getLong(dateTakenCol).takeIf { it > 0 } else null,
                    duration     = if (durationCol >= 0) c.getLong(durationCol).takeIf { it > 0 } else null,
                    width        = c.getInt(widthCol),
                    height       = c.getInt(heightCol),
                    bucketId     = if (bucketIdCol >= 0) c.getLong(bucketIdCol) else 0L,
                    bucketName   = if (bucketNameCol >= 0) c.getString(bucketNameCol) ?: "Unknown" else "Unknown",
                    path         = path,
                    latitude     = if (latCol >= 0) c.getDouble(latCol).takeIf { it != 0.0 } else null,
                    longitude    = if (lonCol >= 0) c.getDouble(lonCol).takeIf { it != 0.0 } else null,
                )
            }
            items
        } ?: emptyList()
    }

    private fun buildProjection(isVideo: Boolean): Array<String> {
        val base = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
        )
        if (isVideo) {
            base += MediaStore.Video.Media.DURATION
        } else {
            base += MediaStore.Images.Media.LATITUDE
            base += MediaStore.Images.Media.LONGITUDE
        }
        return base.toTypedArray()
    }

    private fun SortOrder.toSqlOrderClause(): String = when (this) {
        SortOrder.NEWEST    -> "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        SortOrder.OLDEST    -> "${MediaStore.MediaColumns.DATE_ADDED} ASC"
        SortOrder.NAME_ASC  -> "${MediaStore.MediaColumns.DISPLAY_NAME} ASC"
        SortOrder.NAME_DESC -> "${MediaStore.MediaColumns.DISPLAY_NAME} DESC"
        SortOrder.SIZE_DESC -> "${MediaStore.MediaColumns.SIZE} DESC"
        SortOrder.SIZE_ASC  -> "${MediaStore.MediaColumns.SIZE} ASC"
    }
}
