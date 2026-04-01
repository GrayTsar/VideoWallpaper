package com.graytsar.livewallpaper.core.common.util

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

fun applyWindowInsets(view: View) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = insets.top
        }

        windowInsets
    }
}
