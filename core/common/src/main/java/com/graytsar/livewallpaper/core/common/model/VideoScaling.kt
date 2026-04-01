package com.graytsar.livewallpaper.core.common.model

import androidx.annotation.StringRes
import com.graytsar.livewallpaper.core.common.R

enum class VideoScaling(val value: Int) {
    FIT_CROP(0),
    FIT_TO_SCREEN(1),
    ORIGINAL(2);

    @StringRes
    fun toTranslation(): Int {
        return when (this) {
            FIT_CROP -> R.string.fit_crop
            FIT_TO_SCREEN -> R.string.fit_to_screen
            ORIGINAL -> R.string.original
        }
    }

    companion object {
        fun getTranslations() = listOf(
            FIT_CROP.toTranslation(),
            FIT_TO_SCREEN.toTranslation(),
            ORIGINAL.toTranslation()
        )

        fun from(index: Int) =
            entries.getOrNull(index) ?: throw IllegalArgumentException("Invalid video scale type index $index")
    }
}