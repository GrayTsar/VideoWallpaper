package com.graytsar.livewallpaper.di

import androidx.datastore.core.DataStore
import com.graytsar.livewallpaper.datastore.UserPreferences
import com.graytsar.livewallpaper.repository.UserPreferencesRepository
import com.graytsar.livewallpaper.repository.UserPreferencesRepositoryImpl
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    fun provideUserPreferencesRepository(
        userPreferencesDataStore: DataStore<UserPreferences>
    ): UserPreferencesRepository {
        return UserPreferencesRepositoryImpl(
            userPreferencesDataStore = userPreferencesDataStore
        )
    }
}