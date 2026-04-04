package com.graytsar.livewallpaper.engine

interface WallpaperRenderer {
    fun onSurfaceReady()
    fun onVisibilityChanged(isVisible: Boolean)
    fun onPauseChanged(isPaused: Boolean)
    fun release()
}