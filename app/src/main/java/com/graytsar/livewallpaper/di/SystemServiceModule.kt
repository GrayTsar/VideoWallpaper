package com.graytsar.livewallpaper.di

import android.app.WallpaperManager
import android.content.ContentResolver
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SystemServiceModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext applicationContext: Context): ContentResolver {
        return applicationContext.contentResolver
    }

    @Provides
    @Singleton
    fun provideWallpaperManager(@ApplicationContext applicationContext: Context): WallpaperManager {
        return WallpaperManager.getInstance(applicationContext)
    }
}