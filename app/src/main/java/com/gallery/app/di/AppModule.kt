package com.gallery.app.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.gallery.app.data.local.GalleryDatabase
import com.gallery.app.data.local.dao.*
import com.gallery.app.data.repository.MediaRepositoryImpl
import com.gallery.app.domain.repository.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GalleryDatabase =
        Room.databaseBuilder(context, GalleryDatabase::class.java, "gallery.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideFavoriteDao(db: GalleryDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun provideHiddenDao(db: GalleryDatabase): HiddenMediaDao = db.hiddenMediaDao()
    @Provides fun provideTrashDao(db: GalleryDatabase): TrashDao = db.trashDao()
    @Provides fun provideMediaCacheDao(db: GalleryDatabase): MediaCacheDao = db.mediaCacheDao()
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository
}
