package com.graytsar.livewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

/*
class VideoOpenWithActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_open_with)

        val fUri = intent.data!!

        val sharedPref = this.getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(keyVideo, fUri.toString())
        editor.putBoolean(keyType, true) //isVideo is true
        editor.apply()

        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, VideoWallpaper::class.java))

        startActivity(intent)
        finish()
    }
}
*/
