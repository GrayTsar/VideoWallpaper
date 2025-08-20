package com.graytsar.livewallpaper.repository

import androidx.datastore.core.DataStore
import com.graytsar.livewallpaper.datastore.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


interface UserPreferencesRepository {
    fun readDarkMode(): Flow<Boolean>
    fun readGifScaleType(): Flow<Int>
    fun readVideoEnableAudio(): Flow<Boolean>
    fun readVideoCrop(): Flow<Boolean>
    fun readDoubleTapToPause(): Flow<Boolean>
    fun readPlayOffscreen(): Flow<Boolean>

    suspend fun updateDarkMode(isDarkModeEnabled: Boolean)
    suspend fun updateDaGifScaleType(gifScaleType: Int)
    suspend fun updateVideoEnableAudio(isVideoAudioEnabled: Boolean)
    suspend fun updateVideoCrop(isVideoCrop: Boolean)
    suspend fun updateDoubleTapToPause(isDoubleTapToPauseEnabled: Boolean)
    suspend fun updatePlayOffscreen(isPlayOffscreenEnabled: Boolean)
}

class UserPreferencesRepositoryImpl(
    private val userPreferencesDataStore: DataStore<UserPreferences>
) : UserPreferencesRepository {

    override fun readDarkMode(): Flow<Boolean> = userPreferencesDataStore.data.map { it.darkMode }
    override fun readGifScaleType(): Flow<Int> =
        userPreferencesDataStore.data.map { it.gifScaleType }

    override fun readVideoEnableAudio(): Flow<Boolean> =
        userPreferencesDataStore.data.map { it.videoEnableAudio }

    override fun readVideoCrop(): Flow<Boolean> = userPreferencesDataStore.data.map { it.videoCrop }
    override fun readDoubleTapToPause(): Flow<Boolean> =
        userPreferencesDataStore.data.map { it.doubleTapToPause }

    override fun readPlayOffscreen(): Flow<Boolean> =
        userPreferencesDataStore.data.map { it.playOffscreen }

    override suspend fun updateDarkMode(isDarkModeEnabled: Boolean) {
        userPreferencesDataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setDarkMode(isDarkModeEnabled)
                .build()
        }
    }

    override suspend fun updateDaGifScaleType(gifScaleType: Int) {
        userPreferencesDataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setGifScaleType(gifScaleType)
                .build()
        }
    }

    override suspend fun updateVideoEnableAudio(isVideoAudioEnabled: Boolean) {
        userPreferencesDataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setVideoEnableAudio(isVideoAudioEnabled)
                .build()
        }
    }

    override suspend fun updateVideoCrop(isVideoCrop: Boolean) {
        userPreferencesDataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setVideoCrop(isVideoCrop)
                .build()
        }
    }

    override suspend fun updateDoubleTapToPause(isDoubleTapToPauseEnabled: Boolean) {
        userPreferencesDataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setDoubleTapToPause(isDoubleTapToPauseEnabled)
                .build()
        }
    }

    override suspend fun updatePlayOffscreen(isPlayOffscreenEnabled: Boolean) {
        userPreferencesDataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setPlayOffscreen(isPlayOffscreenEnabled)
                .build()
        }
    }
}