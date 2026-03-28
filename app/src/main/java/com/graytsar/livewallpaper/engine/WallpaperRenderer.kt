package com.graytsar.livewallpaper.engine

interface WallpaperRenderer {
    fun onSurfaceCreated()
    fun onVisibilityChanged(isVisible: Boolean, isPaused: Boolean)
    fun onPauseStateChanged(isPaused: Boolean, isVisible: Boolean)
    fun release()
}