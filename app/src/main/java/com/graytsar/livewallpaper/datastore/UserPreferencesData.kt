@file:OptIn(ExperimentalSerializationApi::class)

package com.graytsar.livewallpaper.datastore

import com.graytsar.livewallpaper.datastore.ImageScalingProto.CENTER
import com.graytsar.livewallpaper.datastore.ImageScalingProto.FIT_TO_SCREEN
import com.graytsar.livewallpaper.datastore.ImageScalingProto.ORIGINAL
import com.graytsar.livewallpaper.datastore.WallpaperTypeProto.IMAGE
import com.graytsar.livewallpaper.datastore.WallpaperTypeProto.VIDEO
import com.graytsar.livewallpaper.engine.EngineSettings
import com.graytsar.livewallpaper.engine.GeneralSettings
import com.graytsar.livewallpaper.engine.ImageSettings
import com.graytsar.livewallpaper.engine.VideoSettings
import com.graytsar.livewallpaper.util.ImageScaling
import com.graytsar.livewallpaper.util.VideoScaling
import com.graytsar.livewallpaper.util.WallpaperFlag
import com.graytsar.livewallpaper.util.WallpaperType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Persisted protobuf-backed user preferences.
 *
 * Schema notes:
 * - Keep existing [ProtoNumber] values stable.
 * - Do not reuse removed field numbers.
 * - Preserve enum entry order to avoid changing persisted values.
 */
@Serializable
data class UserPreferencesData(
    @property:ProtoNumber(1) val appPreference: AppPreference = AppPreference(),
    @property:ProtoNumber(2) val livePreference: List<LivePreference> = emptyList(),
    @property:ProtoNumber(3) val previewPreference: LivePreference = LivePreference(),
    @property:ProtoNumber(4) val imagePreference: ImagePreference = ImagePreference(),
    @property:ProtoNumber(5) val videoPreference: VideoPreference = VideoPreference(),
    @property:ProtoNumber(6) val generalPreference: GeneralPreference = GeneralPreference(),
)

@Serializable
data class AppPreference(
    @property:ProtoNumber(1)
    val forceDarkMode: Boolean = false
)

@Serializable
data class LivePreference(
    @property:ProtoNumber(1)
    val flag: WallpaperFlagProto = WallpaperFlagProto.SYSTEM,
    @property:ProtoNumber(2)
    val type: WallpaperTypeProto = WallpaperTypeProto.NONE,
    @property:ProtoNumber(3)
    val path: String? = null,
)

@Serializable
data class ImagePreference(
    @property:ProtoNumber(1)
    val scaling: ImageScalingProto = FIT_TO_SCREEN
)

@Serializable
data class VideoPreference(
    @property:ProtoNumber(1)
    val isAudioEnabled: Boolean = false,
    @property:ProtoNumber(2)
    val scaling: VideoScalingProto = VideoScalingProto.FIT_CROP
)

@Serializable
data class GeneralPreference(
    @property:ProtoNumber(1)
    val isDoubleTapToPauseEnabled: Boolean = false,
    @property:ProtoNumber(2)
    val isPlayOffscreenEnabled: Boolean = false
)

fun ImagePreference.toDomain() = ImageSettings(
    scaleType = scaling.toDomain()
)

fun VideoPreference.toDomain() = VideoSettings(
    audio = isAudioEnabled,
    videoScaling = scaling.toDomain()
)

fun GeneralPreference.toDomain() = GeneralSettings(
    doubleTapToPause = isDoubleTapToPauseEnabled,
    playOffscreen = isPlayOffscreenEnabled
)

fun UserPreferencesData.toDomain() = EngineSettings(
    image = imagePreference.toDomain(),
    video = videoPreference.toDomain(),
    general = generalPreference.toDomain()
)

@Serializable
enum class WallpaperTypeProto {
    NONE,
    IMAGE,
    VIDEO;

    fun toDomain() = when (this) {
        NONE -> WallpaperType.NONE
        IMAGE -> WallpaperType.IMAGE
        VIDEO -> WallpaperType.VIDEO
    }
}

fun WallpaperType.toProto() = when (this) {
    WallpaperType.NONE -> WallpaperTypeProto.NONE
    WallpaperType.IMAGE -> IMAGE
    WallpaperType.VIDEO -> VIDEO
}

@Serializable
enum class ImageScalingProto {
    FIT_TO_SCREEN,
    CENTER,
    ORIGINAL;

    fun toDomain() = when (this) {
        FIT_TO_SCREEN -> ImageScaling.FIT_TO_SCREEN
        CENTER -> ImageScaling.CENTER
        ORIGINAL -> ImageScaling.ORIGINAL
    }
}

fun ImageScaling.toProto() = when (this) {
    ImageScaling.FIT_TO_SCREEN -> FIT_TO_SCREEN
    ImageScaling.CENTER -> CENTER
    ImageScaling.ORIGINAL -> ORIGINAL
}

@Serializable
enum class VideoScalingProto {
    FIT_CROP,
    FIT_TO_SCREEN,
    ORIGINAL;

    fun toDomain() = when (this) {
        FIT_CROP -> VideoScaling.FIT_CROP
        FIT_TO_SCREEN -> VideoScaling.FIT_TO_SCREEN
        ORIGINAL -> VideoScaling.ORIGINAL
    }
}

fun VideoScaling.toProto() = when (this) {
    VideoScaling.FIT_CROP -> VideoScalingProto.FIT_CROP
    VideoScaling.FIT_TO_SCREEN -> VideoScalingProto.FIT_TO_SCREEN
    VideoScaling.ORIGINAL -> VideoScalingProto.ORIGINAL
}

@Serializable
enum class WallpaperFlagProto {
    SYSTEM,
    LOCK;

    fun toDomain() = when (this) {
        SYSTEM -> WallpaperFlag.SYSTEM
        LOCK -> WallpaperFlag.LOCK
    }
}

fun WallpaperFlag.toProto() = when (this) {
    WallpaperFlag.SYSTEM -> WallpaperFlagProto.SYSTEM
    WallpaperFlag.LOCK -> WallpaperFlagProto.LOCK
}
