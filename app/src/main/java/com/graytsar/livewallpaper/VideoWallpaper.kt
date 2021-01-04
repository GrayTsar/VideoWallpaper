package com.graytsar.livewallpaper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.Surface
import android.view.SurfaceHolder
import androidx.core.net.toUri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import kotlin.Exception

class VideoWallpaper:WallpaperService() {

    override fun onCreate() {
        super.onCreate()

    }

    override fun onCreateEngine(): Engine {
        val sharedPref = getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
        val isVideo = sharedPref.getBoolean(keyType, false)

        return if(isVideo) {
            VideoWallpaperEngine()
        } else {
            ImageWallpaperEngine()
        }
    }

    inner class ImageWallpaperEngine:Engine() {
        private var animatedImageDrawable: Drawable? = null
        private var movie: Movie? = null

        private var holderInstance: SurfaceHolder? = null
        private var drawJob: Job? = null

        private var shouldDraw:Boolean = true

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            holderInstance = holder

            val sharedPref = getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
            var fUri: Uri? = null

            try {
                fUri = Uri.parse(sharedPref.getString(keyVideo, null))
            } catch (e: NullPointerException) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }


            if(isPreview) {
                if(fUri == null) {
                    return
                }

                if(Build.VERSION.SDK_INT >= 28){
                    val source = ImageDecoder.createSource(contentResolver, fUri)
                    animatedImageDrawable = ImageDecoder.decodeDrawable(source)

                    if(animatedImageDrawable is AnimatedImageDrawable){
                        val anim = animatedImageDrawable as AnimatedImageDrawable
                        anim.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        anim.start()
                    }
                } else {
                    movie = Movie.decodeStream(contentResolver.openInputStream(fUri))
                }

            } else {
                val basePath = applicationContext.filesDir.path
                val folder = imageFolder

                //create directories if they do not exist
                val directory = File("$basePath/$folder")
                if(!directory.isDirectory){
                    directory.mkdirs()
                }

                try {
                    val inputStream = contentResolver.openInputStream(fUri!!)!!
                    val fileOutputStream = FileOutputStream("$basePath/$folder/$imageName", false)

                    inputStream.copyTo(fileOutputStream)

                    fileOutputStream.close()
                    inputStream.close()
                } catch (e:Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }

                if(Build.VERSION.SDK_INT >= 28){
                    val source = ImageDecoder.createSource(File("$basePath/$folder/$imageName"))
                    animatedImageDrawable = ImageDecoder.decodeDrawable(source)

                    if(animatedImageDrawable is AnimatedImageDrawable){
                        val anim = animatedImageDrawable as AnimatedImageDrawable
                        anim.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        anim.start()
                    }
                } else {
                    movie = Movie.decodeStream(File("$basePath/$folder/$imageName").inputStream())
                }
            }

            drawJob = startJob()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if(visible) {
                if(Build.VERSION.SDK_INT >= 28){
                    if(animatedImageDrawable is AnimatedImageDrawable){
                        val anim = animatedImageDrawable as AnimatedImageDrawable
                        anim.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        anim.start()
                    }
                }
                shouldDraw = true
                drawJob = startJob()
            } else {
                if(Build.VERSION.SDK_INT >= 28){
                    if(animatedImageDrawable is AnimatedImageDrawable){
                        val anim = animatedImageDrawable as AnimatedImageDrawable
                        anim.stop()
                    }
                }
                shouldDraw = false
                drawJob?.cancel()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            clearImage()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
        }

        override fun onDestroy() {
            super.onDestroy()
            clearImage()
        }

        private fun startJob(): Job {
            return GlobalScope.launch {
                try{
                    while(holderInstance != null && holderInstance!!.surface.isValid && shouldDraw){
                        val canvas = holderInstance?.lockCanvas()

                        if(Build.VERSION.SDK_INT >= 28){
                            animatedImageDrawable?.let { animatedImageDrawable ->
                                val sx = canvas!!.width.toFloat() / animatedImageDrawable.intrinsicWidth.toFloat()
                                val sy = canvas.height.toFloat() / animatedImageDrawable.intrinsicHeight.toFloat()

                                canvas.scale(sx, sy)
                                animatedImageDrawable.draw(canvas)
                            }
                        } else {
                            movie?.let { movie ->
                                val sx = canvas!!.width.toFloat() / movie.width().toFloat()
                                val sy = canvas.height.toFloat() / movie.height().toFloat()
                                canvas.scale(sx, sy)

                                movie.draw(canvas, 0f, 0f)
                                movie.setTime((System.currentTimeMillis() % movie.duration()).toInt())

                            }
                        }
                        holderInstance?.unlockCanvasAndPost(canvas!!)
                        delay(15)
                    }
                } catch (e:Exception){
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }

        private fun clearImage() {
            if(Build.VERSION.SDK_INT >= 28){
                if(animatedImageDrawable is AnimatedImageDrawable) {
                    (animatedImageDrawable as AnimatedImageDrawable).stop()
                }
            }

            shouldDraw = false
            drawJob?.cancel()
            drawJob = null
        }
    }

    inner class VideoWallpaperEngine:Engine() {
        var mediaPlayer: MediaPlayer? = null

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)

            val sharedPref = getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
            var fUri: Uri? = null

            try {
                fUri = Uri.parse(sharedPref.getString(keyVideo, null))
            } catch (e: NullPointerException) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }


            if(isPreview) {
                if(fUri == null) {
                    return
                }

                try {
                    mediaPlayer = MediaPlayer.create(this@VideoWallpaper, fUri, VideoWallpaperSurfaceHolder(holder!!)).apply {
                        isLooping = true
                        setVolume(0f,0f)
                    }
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            } else {
                val basePath = applicationContext.filesDir.path
                val folder = videoFolder

                //create directories if they do not exist
                val directory = File("$basePath/$folder")
                if(!directory.isDirectory){
                    directory.mkdirs()
                }

                try {
                    val inputStream = contentResolver.openInputStream(fUri!!)!!
                    val fileOutputStream = FileOutputStream("$basePath/$folder/$videoName", false)

                    inputStream.copyTo(fileOutputStream)

                    fileOutputStream.close()
                    inputStream.close()
                } catch (e:Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }


                val file = File("${applicationContext.filesDir.path}/$videoFolder").listFiles()
                if(file.isNotEmpty() && file[0].exists()) {
                    mediaPlayer = MediaPlayer.create(this@VideoWallpaper, file[0].toUri(), VideoWallpaperSurfaceHolder(holder!!)).apply {
                        isLooping = true
                        setVolume(0f,0f)
                    }
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if(visible) {
                mediaPlayer?.start()
            } else {
                mediaPlayer?.pause()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            clearVideo()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
        }

        override fun onDestroy() {
            super.onDestroy()
            clearVideo()
        }

        private fun clearVideo() {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }


    private class VideoWallpaperSurfaceHolder(private val holder: SurfaceHolder):SurfaceHolder{
        @SuppressLint("ObsoleteSdkInt")
        override fun setType(type: Int) {
            if(Build.VERSION.SDK_INT <= 11) {
                holder.setType(type)
            }
        }

        override fun getSurface(): Surface {
            return holder.surface
        }

        override fun setSizeFromLayout() {
            holder.setSizeFromLayout()
        }

        override fun lockCanvas(): Canvas {
            return holder.lockCanvas()
        }

        override fun lockCanvas(dirty: Rect?): Canvas {
            return holder.lockCanvas(dirty)
        }

        override fun getSurfaceFrame(): Rect {
            return holder.surfaceFrame
        }

        override fun setFixedSize(width: Int, height: Int) {
            holder.setFixedSize(width, height)
        }

        override fun removeCallback(callback: SurfaceHolder.Callback?) {
            holder.removeCallback(callback)
        }

        override fun isCreating(): Boolean {
            return holder.isCreating
        }

        override fun addCallback(callback: SurfaceHolder.Callback?) {
            holder.addCallback(callback)
        }

        override fun setFormat(format: Int) {
            holder.setFormat(format)
        }

        override fun setKeepScreenOn(screenOn: Boolean) {

        }

        override fun unlockCanvasAndPost(canvas: Canvas?) {
            holder.unlockCanvasAndPost(canvas)
        }
    }
}