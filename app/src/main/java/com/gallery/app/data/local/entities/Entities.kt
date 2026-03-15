package com.gallery.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted favorite record.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: Long,
    val addedAt: Long = System.currentTimeMillis(),
)

/**
 * Hidden (vault) media record.
 */
@Entity(tableName = "hidden_media")
data class HiddenMediaEntity(
    @PrimaryKey val mediaId: Long,
    val originalPath: String,
    val hiddenAt: Long = System.currentTimeMillis(),
)

/**
 * Trashed media record.
 */
@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey val mediaId: Long,
    val originalPath: String,
    val trashedAt: Long = System.currentTimeMillis(),
    val autoDeleteAt: Long = trashedAt + (30L * 24 * 60 * 60 * 1000), // 30 days
)

/**
 * Cached media metadata for faster loading.
 */
@Entity(tableName = "media_cache")
data class MediaCacheEntity(
    @PrimaryKey val mediaId: Long,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val dateAdded: Long,
    val dateTaken: Long?,
    val duration: Long?,
    val width: Int,
    val height: Int,
    val bucketId: Long,
    val bucketName: String,
    val path: String,
    val latitude: Double?,
    val longitude: Double?,
    val cachedAt: Long = System.currentTimeMillis(),
)
