package com.graytsar.livewallpaper.core.common.model

data class ImageEngineSettings(
    val image: ImageSettings,
    val general: GeneralSettings
)

data class VideoEngineSettings(
    val video: VideoSettings,
    val general: GeneralSettings
)

data class ImageSettings(
    val scaleType: ImageScaling
)

data class VideoSettings(
    val audio: Boolean,
    val videoScaling: VideoScaling
)

data class GeneralSettings(
    val doubleTapToPause: Boolean,
    val playOffscreen: Boolean
)