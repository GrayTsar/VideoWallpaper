package com.graytsar.livewallpaper.core.repository.di

import androidx.datastore.core.DataStore
import com.graytsar.livewallpaper.core.datastore.UserPreferencesData
import com.graytsar.livewallpaper.core.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideUserPreferencesRepository(dataStore: DataStore<UserPreferencesData>) = UserPreferencesRepository(
        dataStore = dataStore
    )
}