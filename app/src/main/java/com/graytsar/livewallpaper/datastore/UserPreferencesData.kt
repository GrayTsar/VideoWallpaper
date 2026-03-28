@file:OptIn(ExperimentalSerializationApi::class)

package com.graytsar.livewallpaper.datastore

import com.graytsar.livewallpaper.datastore.GifScaleTypeProto.CENTER
import com.graytsar.livewallpaper.datastore.GifScaleTypeProto.FIT_TO_SCREEN
import com.graytsar.livewallpaper.datastore.GifScaleTypeProto.ORIGINAL
import com.graytsar.livewallpaper.datastore.WallpaperTypeProto.IMAGE
import com.graytsar.livewallpaper.datastore.WallpaperTypeProto.VIDEO
import com.graytsar.livewallpaper.util.GifScaleType
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
    @property:ProtoNumber(2) val wallpaperPreference: WallpaperPreference = WallpaperPreference(),
    @property:ProtoNumber(3) val previewWallpaperPreference: WallpaperPreference = WallpaperPreference(),
    @property:ProtoNumber(4) val enginePreference: EnginePreference = EnginePreference(),
)

@Serializable
data class AppPreference(
    @property:ProtoNumber(1)
    val forceDarkMode: Boolean = false
)

@Serializable
data class WallpaperPreference(
    @property:ProtoNumber(1)
    val pathString: String? = null,
    @property:ProtoNumber(2)
    val wallpaperType: WallpaperTypeProto? = null,
)

@Serializable
data class EnginePreference(
    @property:ProtoNumber(1)
    val gifScaleType: GifScaleTypeProto = FIT_TO_SCREEN,
    @property:ProtoNumber(2)
    val isVideoAudioEnabled: Boolean = false,
    @property:ProtoNumber(3)
    val videoCrop: Boolean = false,
    @property:ProtoNumber(4)
    val isDoubleTapToPauseEnabled: Boolean = false,
    @property:ProtoNumber(5)
    val isPlayOffscreenEnabled: Boolean = false
)

@Serializable
enum class WallpaperTypeProto {
    IMAGE,
    VIDEO;

    fun toDomain() = when (this) {
        IMAGE -> WallpaperType.IMAGE
        VIDEO -> WallpaperType.VIDEO
    }
}

fun WallpaperType.toProto() = when (this) {
    WallpaperType.IMAGE -> IMAGE
    WallpaperType.VIDEO -> VIDEO
}

@Serializable
enum class GifScaleTypeProto {
    FIT_TO_SCREEN,
    CENTER,
    ORIGINAL;

    fun toDomain() = when (this) {
        FIT_TO_SCREEN -> GifScaleType.FIT_TO_SCREEN
        CENTER -> GifScaleType.CENTER
        ORIGINAL -> GifScaleType.ORIGINAL
    }
}

fun GifScaleType.toProto() = when (this) {
    GifScaleType.FIT_TO_SCREEN -> FIT_TO_SCREEN
    GifScaleType.CENTER -> CENTER
    GifScaleType.ORIGINAL -> ORIGINAL
}