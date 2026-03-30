package com.graytsar.livewallpaper.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.graytsar.livewallpaper.datastore.UserPreferencesData
import com.graytsar.livewallpaper.datastore.UserPreferencesSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import javax.inject.Singleton

@OptIn(ExperimentalSerializationApi::class)
@Module
@InstallIn(SingletonComponent::class)
object DatastoreModule {
    @Provides
    @Singleton
    fun provideProtobufEncoder(): ProtoBuf = ProtoBuf

    @Singleton
    @Provides
    fun provideUserPreferencesSerializer(encoder: ProtoBuf): Serializer<UserPreferencesData> =
        UserPreferencesSerializer(encoder = encoder)

    @Singleton
    @Provides
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
        protobufSerializer: Serializer<UserPreferencesData>
    ) = DataStoreFactory.create(
        serializer = protobufSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { UserPreferencesData() }
        ),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { context.dataStoreFile("user_preferences") }
    )
}