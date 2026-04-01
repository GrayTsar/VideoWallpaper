package com.graytsar.livewallpaper.core.repository

import androidx.datastore.core.DataStore
import com.graytsar.livewallpaper.core.common.model.ImageEngineSettings
import com.graytsar.livewallpaper.core.common.model.ImageScaling
import com.graytsar.livewallpaper.core.common.model.VideoEngineSettings
import com.graytsar.livewallpaper.core.common.model.VideoScaling
import com.graytsar.livewallpaper.core.common.model.WallpaperFlag
import com.graytsar.livewallpaper.core.common.model.WallpaperServiceType
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import com.graytsar.livewallpaper.core.datastore.LivePreference
import com.graytsar.livewallpaper.core.datastore.UserPreferencesData
import com.graytsar.livewallpaper.core.repository.domain.toImageEngineSettings
import com.graytsar.livewallpaper.core.repository.domain.toProto
import com.graytsar.livewallpaper.core.repository.domain.toVideoEngineSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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

    suspend fun setPreviewWallpaperService(service: WallpaperServiceType) = update {
        it.copy(previewPreference = it.previewPreference.copy(service = service.toProto()))
    }

    suspend fun setPreviewFlags(flag: WallpaperFlag) = update {
        it.copy(previewPreference = it.previewPreference.copy(flag = flag.toProto()))
    }

    fun getImageEngineSettingsFlow(): Flow<ImageEngineSettings> = dataStore.data.map {
        it.toImageEngineSettings()
    }.distinctUntilChanged()

    fun getVideoEngineSettingsFlow(): Flow<VideoEngineSettings> = dataStore.data.map {
        it.toVideoEngineSettings()
    }.distinctUntilChanged()

    fun getWallpaperSelectionFlow(
        isPreview: Boolean,
        serviceType: WallpaperServiceType,
        wallpaperFlag: WallpaperFlag
    ): Flow<WallpaperSelection?> {
        return dataStore.data.map {
            val liveSelection = it.livePreference.firstNotNullOfOrNull { preference ->
                preference.toSelection()?.takeIf { selection ->
                    selection.service == serviceType && selection.flag == wallpaperFlag
                }
            }
            val previewSelection = it.previewPreference.toSelection()?.takeIf { selection ->
                selection.service == serviceType && selection.flag == wallpaperFlag
            }

            if (isPreview) {
                previewSelection ?: liveSelection
            } else {
                liveSelection
            }
        }.distinctUntilChanged()
    }

    suspend fun promotePreviewSelectionToWallpaper(flag: WallpaperFlag) = update { data ->
        val previewSelection = data.previewPreference
        if (previewSelection.toSelection() == null) {
            //no preview data exists
            data
        } else {
            //copy preview data into live wallpaper data
            val newList =
                data.livePreference.filter { it.flag != flag.toProto() } + previewSelection.copy(flag = flag.toProto())
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
        withContext(Dispatchers.IO) { dataStore.updateData { transform(it) } }
    }
}

private fun LivePreference.toSelection(): WallpaperSelection? {
    val path = path ?: return null
    val type = type.toDomain()
    val flag = flag.toDomain()
    val service = service.toDomain(type)
    return WallpaperSelection(flag = flag, path = path, type = type, service = service)
}

data class WallpaperSelection(
    val flag: WallpaperFlag,
    val path: String,
    val type: WallpaperType,
    val service: WallpaperServiceType
)
