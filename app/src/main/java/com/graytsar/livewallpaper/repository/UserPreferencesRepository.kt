package com.graytsar.livewallpaper.repository

import androidx.datastore.core.DataStore
import com.graytsar.livewallpaper.datastore.UserPreferencesData
import com.graytsar.livewallpaper.datastore.WallpaperTypeProto.Companion.toProto
import com.graytsar.livewallpaper.util.WallpaperType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(
    private val dataStore: DataStore<UserPreferencesData>
) {
    suspend fun getForceDarkMode() = dataStore.data.map {
        it.preference.foreDarkMode
    }.first()

    suspend fun getWallpaperType() = dataStore.data.map {
        it.wallpaperType?.toWallpaperType()
    }.first()

    suspend fun getPreviewPath() = dataStore.data.map {
        it.previewPathString
    }.first()

    suspend fun getWallpaperPath() = dataStore.data.map {
        it.wallpaperPathString
    }.first()

    suspend fun setWallpaperType(type: WallpaperType) = dataStore.updateData {
        it.copy(wallpaperType = type.toProto())
    }

    suspend fun setPreviewPath(path: String?) = dataStore.updateData {
        it.copy(previewPathString = path)
    }

    suspend fun setWallpaperPath(path: String?) = dataStore.updateData {
        it.copy(wallpaperPathString = path)
    }
}