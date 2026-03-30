package com.graytsar.livewallpaper.engine

import android.graphics.Canvas
import android.graphics.Movie
import android.view.SurfaceHolder
import java.io.File

@Suppress("DEPRECATION")
class LegacyImageRenderer(
    holder: SurfaceHolder,
    file: File,
    settings: EngineSettings
) : BaseImageRenderer(holder, file, settings) {
    private var movie: Movie? = null

    override fun loadContent(file: File) {
        file.inputStream().use { inputStream ->
            movie = Movie.decodeStream(inputStream)
        }
    }

    override fun drawOriginal(canvas: Canvas) {
        movie?.let { movie ->
            movie.draw(canvas, 0f, 0f)
            movie.setTime((System.currentTimeMillis() % movie.safeDuration()).toInt())
        }
    }

    override fun drawCenter(canvas: Canvas) {
        movie?.let { movie ->
            val sx = (canvas.width.toFloat() / movie.width().toFloat()) / 2
            val sy = (canvas.height.toFloat() / movie.height().toFloat()) / 2

            movie.draw(canvas, sx, sy)
            movie.setTime((System.currentTimeMillis() % movie.safeDuration()).toInt())
        }
    }

    override fun drawFit(canvas: Canvas) {
        movie?.let { movie ->
            val sx = canvas.width.toFloat() / movie.width().toFloat()
            val sy = canvas.height.toFloat() / movie.height().toFloat()
            canvas.scale(sx, sy)

            movie.draw(canvas, 0f, 0f)
            movie.setTime((System.currentTimeMillis() % movie.safeDuration()).toInt())
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