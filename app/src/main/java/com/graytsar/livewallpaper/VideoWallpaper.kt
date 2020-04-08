package com.graytsar.livewallpaper

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.Context
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Movie
import android.graphics.Rect
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.core.net.toFile
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import java.lang.Exception

class VideoWallpaper:WallpaperService() {
    override fun onCreateEngine(): Engine {
        return object:WallpaperService.Engine(){
            var mediaPlayer:MediaPlayer? = null
            var fUri:String? = null
            var isImage = false

            var hol:SurfaceHolder? = null

            //api < 28
            var movie: Movie? = null

            //api >= 28
            var drawable:Drawable? = null
            var dJob:Job? = null

            init{
                val sharedPref = getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
                fUri = sharedPref.getString(keyVideo, null)
                val extension = fUri!!.substring(fUri!!.lastIndexOf("."))

                if(extension == ".gif" || extension == ".webp" || extension == ".heif"){
                    isImage = true
                }

            }

            override fun onCreate(surfaceHolder: SurfaceHolder?) {
                Log.d("DBG:", "onCreate")
                super.onCreate(surfaceHolder)
            }

            override fun onSurfaceCreated(holder: SurfaceHolder?) {
                Log.d("DBG:", "onSurfaceCreated")
                hol = holder

                if(isImage){
                    if(Build.VERSION.SDK_INT >= 28){
                        val source = ImageDecoder.createSource(contentResolver, Uri.parse(fUri))
                        drawable = ImageDecoder.decodeDrawable(source)

                        if(drawable is AnimatedImageDrawable){
                            val anim = drawable as AnimatedImageDrawable
                            anim.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                            anim.start()
                        }
                    } else  {
                        val c = contentResolver.openInputStream(Uri.parse(fUri))

                        movie = Movie.decodeStream(c)
                    }
                    dJob = startJob()
                } else {
                    mediaPlayer = MediaPlayer.create(this@VideoWallpaper, Uri.parse(fUri), mSurfaceHolder(holder!!)).apply {
                        isLooping = true
                        setVolume(0f,0f)
                    }
                }
                super.onSurfaceCreated(holder)
            }

            override fun onVisibilityChanged(visible: Boolean) {
                Log.d("DBG:", "onVisibilityChanged $visible")

                if(visible){
                    mediaPlayer?.start()

                    if(Build.VERSION.SDK_INT >= 28){
                        if(drawable is AnimatedImageDrawable){
                            val anim = drawable as AnimatedImageDrawable
                            anim.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                            anim.start()
                            dJob = startJob()
                        }
                    }
                } else {
                    mediaPlayer?.pause()

                    if(Build.VERSION.SDK_INT >= 28){
                        if(drawable is AnimatedImageDrawable){
                            val anim = drawable as AnimatedImageDrawable
                            anim.stop()
                            dJob?.cancel()
                        }
                    }
                }

                super.onVisibilityChanged(visible)
            }

            override fun onDestroy() {
                mediaPlayer?.apply {
                    stop()
                    release()
                }

                dJob?.cancel()

                super.onDestroy()
            }

            private fun startJob(): Job {
                return GlobalScope.launch {
                    try{
                        while(hol != null){
                            val canvas = hol?.lockCanvas()

                            if(Build.VERSION.SDK_INT >= 28){
                                drawable?.draw(canvas!!)
                            } else {
                                movie?.let {
                                    it.draw(canvas!!, 0f, 0f)
                                    it.setTime((System.currentTimeMillis() % movie!!.duration()).toInt())
                                }
                            }
                            hol?.unlockCanvasAndPost(canvas!!)
                        }
                    } catch (e:Exception){

                    }
                }
            }
        }
    }

    class mSurfaceHolder(private val holder: SurfaceHolder):SurfaceHolder{
        override fun setType(type: Int) {
            holder.setType(type)
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