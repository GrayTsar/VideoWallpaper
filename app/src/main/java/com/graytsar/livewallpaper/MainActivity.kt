package com.graytsar.livewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

const val keySharedPrefVideo = "video"
const val keyVideo = "key"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    fun onClick(view: View){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(request:Int, result:Int, resultData: Intent?){
        super.onActivityResult(request, result, resultData)
        if(resultData != null && resultData.data != null){
            val fUri = resultData.data!!

            val sharedPref = this.getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString(keyVideo, fUri.toString())
            editor.apply()

            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, VideoWallpaper::class.java))


            startActivity(intent)
        }
    }
}
