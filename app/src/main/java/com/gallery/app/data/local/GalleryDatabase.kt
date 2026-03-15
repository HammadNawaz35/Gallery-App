package com.gallery.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gallery.app.data.local.dao.*
import com.gallery.app.data.local.entities.*

@Database(
    entities = [
        FavoriteEntity::class,
        HiddenMediaEntity::class,
        TrashEntity::class,
        MediaCacheEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun hiddenMediaDao(): HiddenMediaDao
    abstract fun trashDao(): TrashDao
    abstract fun mediaCacheDao(): MediaCacheDao
}
