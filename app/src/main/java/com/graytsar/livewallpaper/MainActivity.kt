package com.graytsar.livewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.firebase.crashlytics.FirebaseCrashlytics

const val keySharedPrefVideo = "video"
const val keyVideo = "key"
const val keyType = "type"

const val videoFolder = "video"
const val imageFolder = "image"

const val videoName = "video"
const val imageName = "image"

class MainActivity : AppCompatActivity() {
    private var isVideo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onClickVideo(view: View){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "video/*"
        isVideo = true
        startActivityForResult(intent, 1)
    }

    fun onClickImage(view: View){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        isVideo = false
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(request:Int, result:Int, resultData: Intent?){
        super.onActivityResult(request, result, resultData)
        resultData?.data?.let {
            try{
                WallpaperManager.getInstance(applicationContext).clear()
            } catch (e:Exception){
                FirebaseCrashlytics.getInstance().recordException(e)
            }

            val fUri = it
            val extension = contentResolver.getType(fUri)

            contentResolver.takePersistableUriPermission(fUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            //problem with selecting multiple mime types. Limits how the SAF "Files" App shows stuff
            //simply add support for non animated images like this
            if(!isVideo && extension != "image/gif" && extension != "image/webp"){
                val manager:WallpaperManager = WallpaperManager.getInstance(applicationContext)
                manager.setStream(contentResolver.openInputStream(fUri))
                return
            }

            val sharedPref = this.getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString(keyVideo, fUri.toString())
            editor.putBoolean(keyType, isVideo)
            editor.apply()

            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, VideoWallpaper::class.java))

            startActivity(intent)
        }
    }
}
