package com.graytsar.livewallpaper.repository

import androidx.datastore.core.DataStore
import com.graytsar.livewallpaper.datastore.UserPreferencesData
import com.graytsar.livewallpaper.datastore.toProto
import com.graytsar.livewallpaper.util.GifScaleType
import com.graytsar.livewallpaper.util.WallpaperType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(
    private val dataStore: DataStore<UserPreferencesData>
) {
    suspend fun getForceDarkMode() = dataStore.data.map {
        it.appPreference.forceDarkMode
    }.first()

    suspend fun setForceDarkMode(enabled: Boolean) = dataStore.updateData {
        it.copy(appPreference = it.appPreference.copy(forceDarkMode = enabled))
    }

    suspend fun getGifScaleType() = dataStore.data.map {
        it.enginePreference.gifScaleType.toDomain()
    }.first()

    suspend fun setGifScaleType(type: GifScaleType) = dataStore.updateData {
        it.copy(enginePreference = it.enginePreference.copy(gifScaleType = type.toProto()))
    }

    suspend fun getVideoAudio() = dataStore.data.map {
        it.enginePreference.isVideoAudioEnabled
    }.first()

    suspend fun setVideoAudio(enabled: Boolean) = dataStore.updateData {
        it.copy(enginePreference = it.enginePreference.copy(isVideoAudioEnabled = enabled))
    }

    suspend fun getVideoCrop() = dataStore.data.map {
        it.enginePreference.videoCrop
    }.first()

    suspend fun setVideoCrop(enabled: Boolean) = dataStore.updateData {
        it.copy(enginePreference = it.enginePreference.copy(videoCrop = enabled))
    }

    suspend fun getDoubleTapToPause() = dataStore.data.map {
        it.enginePreference.isDoubleTapToPauseEnabled
    }.first()

    suspend fun setDoubleTapToPause(enabled: Boolean) = dataStore.updateData {
        it.copy(enginePreference = it.enginePreference.copy(isDoubleTapToPauseEnabled = enabled))
    }

    suspend fun getPlayOffscreen() = dataStore.data.map {
        it.enginePreference.isPlayOffscreenEnabled
    }.first()

    suspend fun setPlayOffscreen(enabled: Boolean) = dataStore.updateData {
        it.copy(enginePreference = it.enginePreference.copy(isPlayOffscreenEnabled = enabled))
    }

    suspend fun getWallpaperType() = dataStore.data.map {
        it.wallpaperPreference.wallpaperType?.toDomain()
    }.first()

    suspend fun setWallpaperType(type: WallpaperType?) = dataStore.updateData {
        it.copy(wallpaperPreference = it.wallpaperPreference.copy(wallpaperType = type?.toProto()))
    }

    suspend fun getWallpaperPath() = dataStore.data.map {
        it.wallpaperPreference.pathString
    }.first()

    suspend fun setWallpaperPath(path: String?) = dataStore.updateData {
        it.copy(wallpaperPreference = it.wallpaperPreference.copy(pathString = path))
    }

    suspend fun getPreviewPath() = dataStore.data.map {
        it.previewWallpaperPreference.pathString
    }.first()

    suspend fun setPreviewPath(path: String?) = dataStore.updateData {
        it.copy(previewWallpaperPreference = it.previewWallpaperPreference.copy(pathString = path))
    }

    suspend fun getPreviewWallpaperType() = dataStore.data.map {
        it.previewWallpaperPreference.wallpaperType?.toDomain()
    }.first()

    suspend fun setPreviewWallpaperType(type: WallpaperType?) = dataStore.updateData {
        it.copy(previewWallpaperPreference = it.previewWallpaperPreference.copy(wallpaperType = type?.toProto()))
    }
}
