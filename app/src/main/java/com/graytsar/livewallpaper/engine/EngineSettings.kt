package com.graytsar.livewallpaper.engine

data class EngineSettings(
    val audio: Boolean,
    val videoCrop: Boolean,
    val scaleType: String,
    val doubleTapToPause: Boolean,
    val playOffscreen: Boolean
)