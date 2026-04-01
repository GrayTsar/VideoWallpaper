package com.graytsar.livewallpaper.core.repository.domain

import com.graytsar.livewallpaper.core.common.model.GeneralSettings
import com.graytsar.livewallpaper.core.common.model.ImageEngineSettings
import com.graytsar.livewallpaper.core.common.model.ImageScaling
import com.graytsar.livewallpaper.core.common.model.ImageSettings
import com.graytsar.livewallpaper.core.common.model.VideoEngineSettings
import com.graytsar.livewallpaper.core.common.model.VideoScaling
import com.graytsar.livewallpaper.core.common.model.VideoSettings
import com.graytsar.livewallpaper.core.common.model.WallpaperFlag
import com.graytsar.livewallpaper.core.common.model.WallpaperServiceType
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import com.graytsar.livewallpaper.core.datastore.GeneralPreference
import com.graytsar.livewallpaper.core.datastore.ImagePreference
import com.graytsar.livewallpaper.core.datastore.ImageScalingProto
import com.graytsar.livewallpaper.core.datastore.UserPreferencesData
import com.graytsar.livewallpaper.core.datastore.VideoPreference
import com.graytsar.livewallpaper.core.datastore.VideoScalingProto
import com.graytsar.livewallpaper.core.datastore.WallpaperFlagProto
import com.graytsar.livewallpaper.core.datastore.WallpaperServiceTypeProto
import com.graytsar.livewallpaper.core.datastore.WallpaperTypeProto

fun WallpaperType.toProto() = when (this) {
    WallpaperType.NONE -> WallpaperTypeProto.NONE
    WallpaperType.IMAGE -> WallpaperTypeProto.IMAGE
    WallpaperType.VIDEO -> WallpaperTypeProto.VIDEO
}

fun WallpaperServiceType.toProto() = when (this) {
    WallpaperServiceType.IMAGE -> WallpaperServiceTypeProto.IMAGE
    WallpaperServiceType.VIDEO -> WallpaperServiceTypeProto.VIDEO
}

fun ImageScaling.toProto() = when (this) {
    ImageScaling.FIT_TO_SCREEN -> ImageScalingProto.FIT_TO_SCREEN
    ImageScaling.CENTER -> ImageScalingProto.CENTER
    ImageScaling.ORIGINAL -> ImageScalingProto.ORIGINAL
}

fun VideoScaling.toProto() = when (this) {
    VideoScaling.FIT_CROP -> VideoScalingProto.FIT_CROP
    VideoScaling.FIT_TO_SCREEN -> VideoScalingProto.FIT_TO_SCREEN
    VideoScaling.ORIGINAL -> VideoScalingProto.ORIGINAL
}

fun WallpaperFlag.toProto() = when (this) {
    WallpaperFlag.SYSTEM -> WallpaperFlagProto.SYSTEM
    WallpaperFlag.LOCK -> WallpaperFlagProto.LOCK
}

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

fun UserPreferencesData.toImageEngineSettings() = ImageEngineSettings(
    image = imagePreference.toDomain(),
    general = generalPreference.toDomain()
)

fun UserPreferencesData.toVideoEngineSettings() = VideoEngineSettings(
    video = videoPreference.toDomain(),
    general = generalPreference.toDomain()
)