package com.graytsar.livewallpaper.repository

import androidx.datastore.core.DataStore
import com.graytsar.livewallpaper.datastore.LivePreference
import com.graytsar.livewallpaper.datastore.UserPreferencesData
import com.graytsar.livewallpaper.datastore.toDomain
import com.graytsar.livewallpaper.datastore.toProto
import com.graytsar.livewallpaper.engine.EngineSettings
import com.graytsar.livewallpaper.util.ImageScaling
import com.graytsar.livewallpaper.util.VideoScaling
import com.graytsar.livewallpaper.util.WallpaperFlag
import com.graytsar.livewallpaper.util.WallpaperType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(
    private val dataStore: DataStore<UserPreferencesData>
) {
    suspend fun getForceDarkMode() = dataStore.data.map {
        it.appPreference.forceDarkMode
    }.first()

    suspend fun setForceDarkMode(enabled: Boolean) = update {
        it.copy(appPreference = it.appPreference.copy(forceDarkMode = enabled))
    }


    suspend fun getImageScaleType() = dataStore.data.map {
        it.imagePreference.scaling.toDomain()
    }.first()

    suspend fun setImageScaleType(type: ImageScaling) = update {
        it.copy(imagePreference = it.imagePreference.copy(scaling = type.toProto()))
    }


    suspend fun getVideoAudio() = dataStore.data.map {
        it.videoPreference.isAudioEnabled
    }.first()

    suspend fun setVideoAudio(enabled: Boolean) = update {
        it.copy(videoPreference = it.videoPreference.copy(isAudioEnabled = enabled))
    }


    suspend fun getVideoScaling() = dataStore.data.map {
        it.videoPreference.scaling.toDomain()
    }.first()

    suspend fun setVideoScaling(scaling: VideoScaling) = update {
        it.copy(videoPreference = it.videoPreference.copy(scaling = scaling.toProto()))
    }


    suspend fun getDoubleTapToPause() = dataStore.data.map {
        it.generalPreference.isDoubleTapToPauseEnabled
    }.first()

    suspend fun setDoubleTapToPause(enabled: Boolean) = update {
        it.copy(generalPreference = it.generalPreference.copy(isDoubleTapToPauseEnabled = enabled))
    }


    suspend fun getPlayOffscreen() = dataStore.data.map {
        it.generalPreference.isPlayOffscreenEnabled
    }.first()

    suspend fun setPlayOffscreen(enabled: Boolean) = update {
        it.copy(generalPreference = it.generalPreference.copy(isPlayOffscreenEnabled = enabled))
    }


    suspend fun getWallpaperPath() = dataStore.data.map {
        it.livePreference.firstOrNull()?.path
    }.first()


    suspend fun getPreviewPath() = dataStore.data.map {
        it.previewPreference.path
    }.first()

    suspend fun setPreviewPath(path: String?) = update {
        it.copy(previewPreference = it.previewPreference.copy(path = path))
    }


    suspend fun setPreviewWallpaperType(type: WallpaperType) = update {
        it.copy(previewPreference = it.previewPreference.copy(type = type.toProto()))
    }

    fun getEngineSettingsFlow(): Flow<EngineSettings> = dataStore.data.map {
        it.toDomain()
    }.distinctUntilChanged()

    fun getWallpaperSelectionFlow(isPreview: Boolean): Flow<WallpaperSelection?> {
        return dataStore.data.map {
            if (isPreview) {
                it.previewPreference.toSelection() ?: it.livePreference.firstOrNull()?.toSelection()
            } else {
                it.livePreference.firstOrNull()?.toSelection()
            }
        }.distinctUntilChanged()
    }

    suspend fun promotePreviewSelectionToWallpaper() = update { data ->
        val previewSelection = data.previewPreference
        if (previewSelection.toSelection() == null) {
            //no preview data exists
            data
        } else {
            //copy preview data into live wallpaper data
            val newList = data.livePreference.filter { it.flag != previewSelection.flag } + previewSelection
            data.copy(
                livePreference = newList,
                previewPreference = LivePreference() // Reset preview after promotion
            )
        }
    }

    suspend fun clearPreviewData() = update {
        it.copy(previewPreference = LivePreference())
    }

    private suspend fun update(transform: suspend (UserPreferencesData) -> UserPreferencesData) {
        dataStore.updateData { transform(it) }
    }
}

private fun LivePreference.toSelection(): WallpaperSelection? {
    val path = path ?: return null
    val type = type?.toDomain() ?: return null
    val flag = flag.toDomain()
    return WallpaperSelection(flag = flag, path = path, type = type)
}

data class WallpaperSelection(
    val flag: WallpaperFlag,
    val path: String,
    val type: WallpaperType
)
