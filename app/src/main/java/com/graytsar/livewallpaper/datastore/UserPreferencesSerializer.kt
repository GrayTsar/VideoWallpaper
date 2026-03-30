package com.graytsar.livewallpaper.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
class UserPreferencesSerializer(val encoder: ProtoBuf) : Serializer<UserPreferencesData> {
    override val defaultValue: UserPreferencesData
        get() = UserPreferencesData()

    override suspend fun readFrom(input: InputStream): UserPreferencesData = try {
        encoder.decodeFromByteArray(UserPreferencesData.serializer(), input.readBytes())
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Cannot read protobuf file.", e)
    }

    override suspend fun writeTo(t: UserPreferencesData, output: OutputStream) {
        withContext(Dispatchers.IO) {
            kotlin.runCatching { output.write(encoder.encodeToByteArray(UserPreferencesData.serializer(), t)) }
        }
    }
}
