package com.graytsar.livewallpaper.core.common.model

enum class WallpaperFlag {
    SYSTEM,
    LOCK;

    companion object {
        fun from(which: Int): WallpaperFlag {
            return when (which) {
                1 -> SYSTEM
                2 -> LOCK
                else -> SYSTEM
            }
        }
    }
}