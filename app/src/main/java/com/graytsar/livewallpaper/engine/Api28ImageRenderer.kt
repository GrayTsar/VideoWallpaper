package com.graytsar.livewallpaper.engine

import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import com.graytsar.livewallpaper.core.common.model.ImageEngineSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.P)
class Api28ImageRenderer(
    holder: SurfaceHolder,
    file: File,
    settings: ImageEngineSettings
) : BaseImageRenderer(holder, file, settings) {
    private var animatedImageDrawable: Drawable? = null

    override fun loadContent(file: File) {
        val source = ImageDecoder.createSource(file)
        animatedImageDrawable = runBlocking(Dispatchers.IO) {
            ImageDecoder.decodeDrawable(source)
        }

        startAnimationIfNeeded()
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        val animation = animatedImageDrawable as? AnimatedImageDrawable ?: return
        if (isPlaying) {
            animation.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            animation.start()
        } else {
            animation.stop()
        }
    }

    override fun drawFitCrop(canvas: Canvas) {
        animatedImageDrawable?.apply {
            val drawableWidth = intrinsicWidth
            val drawableHeight = intrinsicHeight
            if (drawableWidth <= 0 || drawableHeight <= 0) return@apply

            val scale = max(
                canvas.width.toFloat() / drawableWidth.toFloat(),
                canvas.height.toFloat() / drawableHeight.toFloat()
            )
            val scaledWidth = (drawableWidth * scale).roundToInt()
            val scaledHeight = (drawableHeight * scale).roundToInt()
            val left = ((canvas.width - scaledWidth) / 2f).roundToInt()
            val top = ((canvas.height - scaledHeight) / 2f).roundToInt()

            setBounds(left, top, left + scaledWidth, top + scaledHeight)
            draw(canvas)
        }
    }

    override fun drawFitToScreen(canvas: Canvas) {
        animatedImageDrawable?.apply {
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
        }
    }

    override fun drawCenter(canvas: Canvas) {
        animatedImageDrawable?.apply {
            val iw = intrinsicWidth.coerceAtLeast(0)
            val ih = intrinsicHeight.coerceAtLeast(0)

            val left = (canvas.width - iw) / 2
            val top = (canvas.height - ih) / 2

            setBounds(left, top, left + iw, top + ih)
            draw(canvas)
        }
    }

    override fun drawOriginal(canvas: Canvas) {
        animatedImageDrawable?.apply {
            val width = intrinsicWidth.coerceAtLeast(0)
            val height = intrinsicHeight.coerceAtLeast(0)
            setBounds(0, 0, width, height)
            draw(canvas)
        }
    }

    override fun cleanupContent() {
        (animatedImageDrawable as? AnimatedImageDrawable)?.stop()
        animatedImageDrawable = null
    }

    private fun startAnimationIfNeeded() {
        val animation = animatedImageDrawable as? AnimatedImageDrawable ?: return
        animation.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
        animation.start()
    }
}