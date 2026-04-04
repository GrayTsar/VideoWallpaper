@file:OptIn(ExperimentalSerializationApi::class)

package com.graytsar.livewallpaper.core.datastore

import com.graytsar.livewallpaper.core.common.model.ImageScaling
import com.graytsar.livewallpaper.core.common.model.VideoScaling
import com.graytsar.livewallpaper.core.common.model.WallpaperFlag
import com.graytsar.livewallpaper.core.common.model.WallpaperServiceType
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Persisted protobuf-backed user preferences.
 *
 * Schema notes:
 * - Keep existing [kotlinx.serialization.protobuf.ProtoNumber] values stable.
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
    @property:ProtoNumber(4)
    val service: WallpaperServiceTypeProto = WallpaperServiceTypeProto.UNSPECIFIED,
)

@Serializable
data class ImagePreference(
    @property:ProtoNumber(1)
    val scaling: ImageScalingProto = ImageScalingProto.FIT_TO_SCREEN
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


@Serializable
enum class WallpaperServiceTypeProto {
    UNSPECIFIED,
    IMAGE,
    VIDEO;

    fun toDomain(type: WallpaperType) = when (this) {
        UNSPECIFIED -> when (type) {
            WallpaperType.IMAGE -> WallpaperServiceType.IMAGE
            WallpaperType.NONE, WallpaperType.VIDEO -> WallpaperServiceType.VIDEO
        }

        IMAGE -> WallpaperServiceType.IMAGE
        VIDEO -> WallpaperServiceType.VIDEO
    }
}

@Serializable
enum class ImageScalingProto {
    FIT_CROP,
    FIT_TO_SCREEN,
    CENTER,
    ORIGINAL;

    fun toDomain() = when (this) {
        FIT_CROP -> ImageScaling.FIT_CROP
        FIT_TO_SCREEN -> ImageScaling.FIT_TO_SCREEN
        CENTER -> ImageScaling.CENTER
        ORIGINAL -> ImageScaling.ORIGINAL
    }
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

@Serializable
enum class WallpaperFlagProto {
    SYSTEM,
    LOCK;

    fun toDomain() = when (this) {
        SYSTEM -> WallpaperFlag.SYSTEM
        LOCK -> WallpaperFlag.LOCK
    }
}