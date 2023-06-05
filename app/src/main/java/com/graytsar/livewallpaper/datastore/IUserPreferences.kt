package com.graytsar.livewallpaper.datastore

import kotlinx.coroutines.flow.Flow

interface IUserPreferences {
    suspend fun saveForceDarkMode(b: Boolean)

    suspend fun readForceDarkMode(): Flow<Boolean>
}