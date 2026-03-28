package com.graytsar.livewallpaper.engine

import com.graytsar.livewallpaper.util.GifScaleType

data class EngineSettings(
    val audio: Boolean,
    val videoCrop: Boolean,
    val scaleType: GifScaleType,
    val doubleTapToPause: Boolean,
    val playOffscreen: Boolean
)