package com.graytsar.livewallpaper.engine

import com.graytsar.livewallpaper.util.ImageScaling
import com.graytsar.livewallpaper.util.VideoScaling

data class EngineSettings(
    val image: ImageSettings,
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