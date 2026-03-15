package com.gallery.app.data.local.dao

import androidx.room.*
import com.gallery.app.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT mediaId FROM favorites")
    fun getAllFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun removeFavorite(mediaId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
    fun isFavorite(mediaId: Long): Flow<Boolean>

    @Query("DELETE FROM favorites WHERE mediaId IN (:ids)")
    suspend fun removeFavorites(ids: List<Long>)
}

@Dao
interface HiddenMediaDao {
    @Query("SELECT * FROM hidden_media ORDER BY hiddenAt DESC")
    fun getHiddenMedia(): Flow<List<HiddenMediaEntity>>

    @Query("SELECT mediaId FROM hidden_media")
    fun getAllHiddenIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideMedia(entity: HiddenMediaEntity)

    @Query("DELETE FROM hidden_media WHERE mediaId = :mediaId")
    suspend fun unhideMedia(mediaId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM hidden_media WHERE mediaId = :mediaId)")
    suspend fun isHidden(mediaId: Long): Boolean
}

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash ORDER BY trashedAt DESC")
    fun getTrashedMedia(): Flow<List<TrashEntity>>

    @Query("SELECT mediaId FROM trash")
    fun getAllTrashedIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun trashMedia(entity: TrashEntity)

    @Query("DELETE FROM trash WHERE mediaId = :mediaId")
    suspend fun restoreMedia(mediaId: Long)

    @Query("DELETE FROM trash WHERE mediaId IN (:ids)")
    suspend fun deleteFromTrash(ids: List<Long>)

    @Query("DELETE FROM trash WHERE autoDeleteAt <= :now")
    suspend fun purgeExpiredTrash(now: Long = System.currentTimeMillis())

    @Query("SELECT EXISTS(SELECT 1 FROM trash WHERE mediaId = :mediaId)")
    suspend fun isTrashed(mediaId: Long): Boolean
}

@Dao
interface MediaCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaCacheEntity>)

    @Query("SELECT * FROM media_cache ORDER BY dateAdded DESC")
    fun getAllCached(): Flow<List<MediaCacheEntity>>

    @Query("DELETE FROM media_cache")
    suspend fun clearCache()

    @Query("SELECT COUNT(*) FROM media_cache")
    suspend fun count(): Int
}
