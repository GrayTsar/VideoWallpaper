package com.graytsar.livewallpaper.core.common.model

import androidx.annotation.StringRes
import com.graytsar.livewallpaper.core.common.R

enum class ImageScaling(val value: Int) {
    FIT_TO_SCREEN(0),
    CENTER(1),
    ORIGINAL(2);

    @StringRes
    fun toTranslation(): Int {
        return when (this) {
            FIT_TO_SCREEN -> R.string.fit_to_screen
            CENTER -> R.string.center
            ORIGINAL -> R.string.original
        }
    }

    companion object {
        fun getTranslations() = listOf(
            FIT_TO_SCREEN.toTranslation(),
            CENTER.toTranslation(),
            ORIGINAL.toTranslation()
        )

        fun from(index: Int) =
            entries.getOrNull(index) ?: throw IllegalArgumentException("Invalid image scale type index $index")
    }
}