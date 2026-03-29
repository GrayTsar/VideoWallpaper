package com.graytsar.livewallpaper.repository

import androidx.datastore.core.DataStore
import com.graytsar.livewallpaper.datastore.UserPreferencesData
import com.graytsar.livewallpaper.datastore.WallpaperPreference
import com.graytsar.livewallpaper.datastore.toDomain
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

    suspend fun getEngineSettings() = dataStore.data.map {
        it.enginePreference.toDomain()
    }.first()

    /**
     * Resolves the selection that a wallpaper engine should render.
     *
     * Preview engines prefer preview data and fall back to the persisted wallpaper.
     * Live engines consume any pending preview selection so the applied wallpaper is available
     * immediately, even before the picker activity receives its result callback.
     */
    suspend fun resolveSelectionForEngine(isPreview: Boolean): WallpaperSelection? {
        return if (isPreview) {
            dataStore.data.map {
                it.previewWallpaperPreference.toSelection() ?: it.wallpaperPreference.toSelection()
            }.first()
        } else {
            dataStore.updateData {
                val previewSelection = it.previewWallpaperPreference.toSelection()
                if (previewSelection == null) {
                    it
                } else {
                    it.copy(
                        wallpaperPreference = it.previewWallpaperPreference,
                        previewWallpaperPreference = WallpaperPreference()
                    )
                }
            }.wallpaperPreference.toSelection()
        }
    }

    suspend fun promotePreviewSelectionToWallpaper() = dataStore.updateData {
        val previewSelection = it.previewWallpaperPreference
        if (previewSelection.toSelection() == null) {
            //no preview data exists
            it
        } else {
            //copy preview data into live wallpaper data
            it.copy(
                wallpaperPreference = previewSelection,
                previewWallpaperPreference = WallpaperPreference()
            )
        }
    }

    suspend fun clearPreviewData() = dataStore.updateData {
        it.copy(previewWallpaperPreference = WallpaperPreference())
    }
}

private fun WallpaperPreference.toSelection(): WallpaperSelection? {
    val path = pathString ?: return null
    val type = wallpaperType?.toDomain() ?: return null
    return WallpaperSelection(path = path, wallpaperType = type)
}

data class WallpaperSelection(
    val path: String,
    val wallpaperType: WallpaperType
)