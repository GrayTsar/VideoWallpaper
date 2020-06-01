package com.graytsar.livewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast

/*
class ImageOpenWithActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_with)

        val fUri = intent.data!!

        val extension = contentResolver.getType(fUri)

        val sharedPref = this.getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(keyVideo, fUri.toString())
        editor.putBoolean(keyType, false) //isVideo is false
        editor.apply()

        //problem with selecting multiple mime types. Limits how the SAF "Files" App shows stuff
        //simply add support for non animated images like this
        if(extension != "image/gif" && extension != "image/webp"){
            val manager: WallpaperManager = WallpaperManager.getInstance(applicationContext)
            manager.setStream(contentResolver.openInputStream(fUri))

            Toast.makeText(applicationContext, "Image Set!", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, VideoWallpaper::class.java))

        startActivity(intent)
        finish()
    }
}
*/
