package com.graytsar.livewallpaper.engine

import android.graphics.Canvas
import android.graphics.Movie
import android.view.SurfaceHolder
import androidx.core.graphics.withSave
import com.graytsar.livewallpaper.core.common.model.ImageEngineSettings
import java.io.File
import kotlin.math.max

@Suppress("DEPRECATION")
class LegacyImageRenderer(
    holder: SurfaceHolder,
    file: File,
    settings: ImageEngineSettings
) : BaseImageRenderer(holder, file, settings) {
    private var movie: Movie? = null

    override fun loadContent(file: File) {
        file.inputStream().use { inputStream ->
            movie = Movie.decodeStream(inputStream)
        }
    }

    override fun drawFitCrop(canvas: Canvas) {
        movie?.apply {
            val movieWidth = width()
            val movieHeight = height()
            if (movieWidth <= 0 || movieHeight <= 0) return@apply

            val scale = max(
                canvas.width.toFloat() / movieWidth.toFloat(),
                canvas.height.toFloat() / movieHeight.toFloat()
            )
            val dx = (canvas.width - (movieWidth * scale)) / 2f
            val dy = (canvas.height - (movieHeight * scale)) / 2f

            setTime((System.currentTimeMillis() % safeDuration()).toInt())

            canvas.withSave {
                translate(dx, dy)
                scale(scale, scale)
                draw(canvas, 0f, 0f)
            }
        }
    }

    override fun drawFitToScreen(canvas: Canvas) {
        movie?.apply {
            val movieWidth = width()
            val movieHeight = height()
            if (movieWidth <= 0 || movieHeight <= 0) return@apply

            val sx = canvas.width.toFloat() / movieWidth.toFloat()
            val sy = canvas.height.toFloat() / movieHeight.toFloat()

            setTime((System.currentTimeMillis() % safeDuration()).toInt())

            canvas.withSave {
                scale(sx, sy)
                draw(canvas, 0f, 0f)
            }
        }
    }

    override fun drawCenter(canvas: Canvas) {
        movie?.apply {
            val dx = (canvas.width - width()) / 2f
            val dy = (canvas.height - height()) / 2f

            setTime((System.currentTimeMillis() % safeDuration()).toInt())

            draw(canvas, dx, dy)
        }
    }

    override fun drawOriginal(canvas: Canvas) {
        movie?.let { movie ->
            movie.setTime((System.currentTimeMillis() % movie.safeDuration()).toInt())

            movie.draw(canvas, 0f, 0f)
        }
    }

    override fun cleanupContent() {
        movie = null
    }

    private fun Movie.safeDuration(): Long {
        val duration = duration()
        return if (duration > 0) {
            duration.toLong()
        } else {
            1000L
        }
    }
}