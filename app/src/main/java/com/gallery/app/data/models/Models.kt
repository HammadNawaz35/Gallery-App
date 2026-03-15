package com.gallery.app.data.models

/**
 * Core media item representing a photo or video on the device.
 */
data class MediaItem(
    val id: Long,
    val uri: android.net.Uri,
    val displayName: String,
    val mimeType: String,
    val size: Long,          // bytes
    val dateAdded: Long,     // seconds since epoch
    val dateModified: Long,
    val dateTaken: Long?,
    val duration: Long?,     // ms – null for images
    val width: Int,
    val height: Int,
    val bucketId: Long,
    val bucketName: String,
    val path: String,
    val latitude: Double?,
    val longitude: Double?,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isGif: Boolean get() = mimeType == "image/gif"
    val isRaw: Boolean get() = mimeType in RAW_TYPES
    val isHeif: Boolean get() = mimeType in HEIF_TYPES

    companion object {
        val RAW_TYPES = setOf(
            "image/x-adobe-dng", "image/x-canon-cr2", "image/x-canon-crw",
            "image/x-nikon-nef", "image/x-sony-arw", "image/x-panasonic-rw2",
            "image/x-olympus-orf", "image/x-fuji-raf", "image/x-raw"
        )
        val HEIF_TYPES = setOf("image/heif", "image/heic", "image/heif-sequence")
    }
}

/**
 * Album grouping of media items by storage bucket.
 */
data class Album(
    val id: Long,
    val name: String,
    val coverUri: android.net.Uri?,
    val mediaCount: Int,
    val dateModified: Long,
    val path: String,
)

/**
 * Sorting options for media grids.
 */
enum class SortOrder {
    NEWEST, OLDEST, NAME_ASC, NAME_DESC, SIZE_DESC, SIZE_ASC
}

/**
 * Media type filter.
 */
enum class MediaFilter {
    ALL, IMAGES, VIDEOS, FAVORITES, HIDDEN, TRASHED
}

/**
 * Vault security type.
 */
enum class VaultAuthType {
    NONE, PIN, BIOMETRIC
}

/**
 * Grid column count options.
 */
enum class GridSize(val columns: Int) {
    SMALL(2), MEDIUM(3), LARGE(4), XLARGE(5)
}

/**
 * Slideshow speed options (ms between transitions).
 */
enum class SlideshowSpeed(val delayMs: Long, val label: String) {
    SLOW(5000, "Slow"),
    NORMAL(3000, "Normal"),
    FAST(1500, "Fast")
}

/**
 * User preferences stored in DataStore.
 */
data class AppPreferences(
    val darkMode: Boolean = false,
    val dynamicColor: Boolean = true,
    val gridSize: GridSize = GridSize.MEDIUM,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val slideshowSpeed: SlideshowSpeed = SlideshowSpeed.NORMAL,
    val vaultPin: String? = null,
    val vaultAuthType: VaultAuthType = VaultAuthType.NONE,
    val isVaultUnlocked: Boolean = false,
)

/**
 * Metadata extracted from EXIF / MediaStore.
 */
data class MediaMetadata(
    val mediaId: Long,
    val resolution: String,
    val fileSize: String,
    val mimeType: String,
    val dateTaken: String?,
    val cameraMake: String?,
    val cameraModel: String?,
    val aperture: String?,
    val shutterSpeed: String?,
    val iso: String?,
    val focalLength: String?,
    val gpsLatitude: Double?,
    val gpsLongitude: Double?,
    val locationName: String?,
    val duration: String?,  // formatted for video
    val width: Int,
    val height: Int,
    val orientation: Int?,
    val colorSpace: String?,
)

/**
 * Duplicate group returned by the DuplicateCleaner.
 */
data class DuplicateGroup(
    val hash: String,
    val items: List<MediaItem>,
    val wastedBytes: Long,
)

/**
 * Search query with optional filters.
 */
data class SearchQuery(
    val text: String = "",
    val mediaFilter: MediaFilter = MediaFilter.ALL,
    val dateRange: Pair<Long, Long>? = null,
    val minSize: Long? = null,
    val maxSize: Long? = null,
)
