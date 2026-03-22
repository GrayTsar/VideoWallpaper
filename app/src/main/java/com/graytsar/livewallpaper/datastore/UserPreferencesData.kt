@file:OptIn(ExperimentalSerializationApi::class)

package com.graytsar.livewallpaper.datastore

import com.graytsar.livewallpaper.util.WallpaperType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class UserPreferencesData(
    @property:ProtoNumber(1) val preference: UserPreference = UserPreference(),
    @property:ProtoNumber(2) val wallpaperType: WallpaperTypeProto? = null,
    @property:ProtoNumber(3) val previewPathString: String? = null,
    @property:ProtoNumber(4) val wallpaperPathString: String? = null
) {
    companion object {
        @Serializable
        data class UserPreference(
            @ProtoNumber(1)
            val foreDarkMode: Boolean = false
        )
    }

}

@Serializable
enum class WallpaperTypeProto {
    IMAGE,
    VIDEO;

    fun toWallpaperType(): WallpaperType {
        return when (this) {
            IMAGE -> WallpaperType.IMAGE
            VIDEO -> WallpaperType.VIDEO
        }
    }

    companion object {
        fun WallpaperType.toProto(): WallpaperTypeProto {
            return when (this) {
                WallpaperType.IMAGE -> IMAGE
                WallpaperType.VIDEO -> VIDEO
            }
        }
    }
}