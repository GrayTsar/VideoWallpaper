package com.graytsar.livewallpaper.engine

import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File

@RequiresApi(Build.VERSION_CODES.P)
class Api28ImageRenderer(
    holder: SurfaceHolder,
    file: File,
    settings: EngineSettings
) : BaseImageRenderer(holder, file, settings) {
    private var animatedImageDrawable: Drawable? = null

    override fun loadContent(file: File) {
        val source = ImageDecoder.createSource(file)
        animatedImageDrawable = runBlocking(Dispatchers.IO) {
            ImageDecoder.decodeDrawable(source)
        }

        startAnimationIfNeeded()
    }

    override fun onImageVisibilityChanged(visible: Boolean) {
        val animation = animatedImageDrawable as? AnimatedImageDrawable ?: return
        if (visible) {
            animation.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            animation.start()
        } else {
            animation.stop()
        }
    }

    override fun drawOriginal(canvas: Canvas) {
        animatedImageDrawable?.draw(canvas)
    }

    override fun drawCenter(canvas: Canvas) {
        animatedImageDrawable?.let { drawable ->
            val sx = (canvas.width.toFloat() - drawable.intrinsicWidth.toFloat()) / 2
            val sy = (canvas.height.toFloat() - drawable.intrinsicHeight.toFloat()) / 2

            canvas.translate(sx, sy)
            drawable.draw(canvas)
        }
    }

    override fun drawFit(canvas: Canvas) {
        animatedImageDrawable?.let { drawable ->
            var ax = drawable.intrinsicWidth.toFloat()
            var ay = drawable.intrinsicHeight.toFloat()

            if (ax <= 0) {
                ax = 1.0f
            }
            if (ay <= 0) {
                ay = 1.0f
            }

            val sx = canvas.width.toFloat() / ax
            val sy = canvas.height.toFloat() / ay

            canvas.scale(sx, sy)
            drawable.draw(canvas)
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