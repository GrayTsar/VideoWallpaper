package com.graytsar.livewallpaper.di

import android.content.Context
import androidx.datastore.core.DataStore
import com.graytsar.livewallpaper.datastore.UserPreferences
import com.graytsar.livewallpaper.datastore.userPreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserPreferencesModule {

    @Singleton
    @Provides
    fun provideUserPreferencesDataStore(@ApplicationContext context: Context): DataStore<UserPreferences> {
        return context.userPreferencesDataStore
    }
}