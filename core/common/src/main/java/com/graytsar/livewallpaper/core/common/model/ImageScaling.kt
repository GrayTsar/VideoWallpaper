package com.graytsar.livewallpaper.core.common.model

import androidx.annotation.StringRes
import com.graytsar.livewallpaper.core.common.R

enum class ImageScaling(val value: Int) {
    FIT_CROP(0),
    FIT_TO_SCREEN(1),
    CENTER(2),
    ORIGINAL(3);

    @StringRes
    fun toTranslation(): Int {
        return when (this) {
            FIT_CROP -> R.string.fit_crop
            FIT_TO_SCREEN -> R.string.fit_to_screen
            CENTER -> R.string.center
            ORIGINAL -> R.string.original
        }
    }

    companion object {
        fun getTranslations() = listOf(
            FIT_CROP.toTranslation(),
            FIT_TO_SCREEN.toTranslation(),
            CENTER.toTranslation(),
            ORIGINAL.toTranslation()
        )

        fun from(index: Int) =
            entries.getOrNull(index) ?: throw IllegalArgumentException("Invalid image scale type index $index")
    }
}