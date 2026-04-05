package com.graytsar.livewallpaper.core.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import app.cash.turbine.test
import com.graytsar.livewallpaper.core.common.model.GeneralSettings
import com.graytsar.livewallpaper.core.common.model.ImageEngineSettings
import com.graytsar.livewallpaper.core.common.model.ImageScaling
import com.graytsar.livewallpaper.core.common.model.ImageSettings
import com.graytsar.livewallpaper.core.common.model.VideoEngineSettings
import com.graytsar.livewallpaper.core.common.model.VideoScaling
import com.graytsar.livewallpaper.core.common.model.VideoSettings
import com.graytsar.livewallpaper.core.common.model.WallpaperFlag
import com.graytsar.livewallpaper.core.common.model.WallpaperServiceType
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import com.graytsar.livewallpaper.core.datastore.LivePreference
import com.graytsar.livewallpaper.core.datastore.UserPreferencesData
import com.graytsar.livewallpaper.core.datastore.UserPreferencesSerializer
import com.graytsar.livewallpaper.core.datastore.WallpaperServiceTypeProto
import com.graytsar.livewallpaper.core.repository.domain.toProto
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
class UserPreferencesRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `settings setters persist scalar preferences`() = runTest {
        val repository = createRepository("scalar_settings.preferences_pb", backgroundScope)

        repository.setForceDarkMode(true)
        repository.setImageScaleType(ImageScaling.CENTER)
        repository.setVideoAudio(true)
        repository.setVideoScaling(VideoScaling.ORIGINAL)
        repository.setDoubleTapToPause(true)
        repository.setPlayOffscreen(true)

        repository.getForceDarkMode() shouldBe true
        repository.getImageScaleType() shouldBe ImageScaling.CENTER
        repository.getVideoAudio() shouldBe true
        repository.getVideoScaling() shouldBe VideoScaling.ORIGINAL
        repository.getDoubleTapToPause() shouldBe true
        repository.getPlayOffscreen() shouldBe true
    }

    @Test
    fun `image engine settings flow reflects stored image and general preferences`() = runTest {
        val repository = createRepository("image_engine.preferences_pb", backgroundScope)

        repository.setImageScaleType(ImageScaling.ORIGINAL)
        repository.setDoubleTapToPause(true)
        repository.setPlayOffscreen(true)

        repository.getImageEngineSettingsFlow().test {
            awaitItem() shouldBe ImageEngineSettings(
                image = ImageSettings(scaleType = ImageScaling.ORIGINAL),
                general = GeneralSettings(
                    doubleTapToPause = true,
                    playOffscreen = true
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `video engine settings flow reflects stored video and general preferences`() = runTest {
        val repository = createRepository("video_engine.preferences_pb", backgroundScope)

        repository.setVideoAudio(true)
        repository.setVideoScaling(VideoScaling.FIT_TO_SCREEN)
        repository.setDoubleTapToPause(true)
        repository.setPlayOffscreen(true)

        repository.getVideoEngineSettingsFlow().test {
            awaitItem() shouldBe VideoEngineSettings(
                video = VideoSettings(
                    audio = true,
                    videoScaling = VideoScaling.FIT_TO_SCREEN
                ),
                general = GeneralSettings(
                    doubleTapToPause = true,
                    playOffscreen = true
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `preview setters persist selection exposed through preview flow`() = runTest {
        val repository = createRepository("preview_selection.preferences_pb", backgroundScope)

        repository.setPreviewWallpaperType(WallpaperType.VIDEO)
        repository.setPreviewWallpaperService(WallpaperServiceType.VIDEO)
        repository.setPreviewPath("/preview/video.mp4")

        repository.getPreviewPath() shouldBe "/preview/video.mp4"
        repository.getWallpaperSelectionFlow(
            isPreview = true,
            serviceType = WallpaperServiceType.VIDEO,
            wallpaperFlag = WallpaperFlag.SYSTEM
        ).test {
            awaitItem() shouldBe WallpaperSelection(
                flag = WallpaperFlag.SYSTEM,
                path = "/preview/video.mp4",
                type = WallpaperType.VIDEO,
                service = WallpaperServiceType.VIDEO
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `preview flow prefers preview selection over live selection`() = runTest {
        val dataStore = createDataStore("preview_preferred.preferences_pb", backgroundScope)
        val repository = UserPreferencesRepository(dataStore)

        dataStore.updateData {
            it.copy(
                livePreference = listOf(
                    LivePreference(
                        flag = WallpaperFlag.SYSTEM.toProto(),
                        type = WallpaperType.IMAGE.toProto(),
                        path = "/live/image.png",
                        service = WallpaperServiceType.IMAGE.toProto()
                    )
                ),
                previewPreference = LivePreference(
                    type = WallpaperType.IMAGE.toProto(),
                    path = "/preview/image.png",
                    service = WallpaperServiceTypeProto.UNSPECIFIED
                )
            )
        }

        repository.getWallpaperSelectionFlow(
            isPreview = true,
            serviceType = WallpaperServiceType.IMAGE,
            wallpaperFlag = WallpaperFlag.SYSTEM
        ).test {
            awaitItem() shouldBe WallpaperSelection(
                flag = WallpaperFlag.SYSTEM,
                path = "/preview/image.png",
                type = WallpaperType.IMAGE,
                service = WallpaperServiceType.IMAGE
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `preview flow falls back to live selection when preview does not match query`() = runTest {
        val dataStore = createDataStore("preview_fallback.preferences_pb", backgroundScope)
        val repository = UserPreferencesRepository(dataStore)

        dataStore.updateData {
            it.copy(
                livePreference = listOf(
                    LivePreference(
                        flag = WallpaperFlag.SYSTEM.toProto(),
                        type = WallpaperType.IMAGE.toProto(),
                        path = "/live/image.png",
                        service = WallpaperServiceType.IMAGE.toProto()
                    )
                ),
                previewPreference = LivePreference(
                    type = WallpaperType.VIDEO.toProto(),
                    path = "/preview/video.mp4",
                    service = WallpaperServiceType.VIDEO.toProto()
                )
            )
        }

        repository.getWallpaperSelectionFlow(
            isPreview = true,
            serviceType = WallpaperServiceType.IMAGE,
            wallpaperFlag = WallpaperFlag.SYSTEM
        ).test {
            awaitItem() shouldBe WallpaperSelection(
                flag = WallpaperFlag.SYSTEM,
                path = "/live/image.png",
                type = WallpaperType.IMAGE,
                service = WallpaperServiceType.IMAGE
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `promotePreviewSelectionToWallpaper replaces matching live flag and clears preview`() = runTest {
        val dataStore = createDataStore("promote_preview.preferences_pb", backgroundScope)
        val repository = UserPreferencesRepository(dataStore)

        dataStore.updateData {
            it.copy(
                livePreference = listOf(
                    LivePreference(
                        flag = WallpaperFlag.SYSTEM.toProto(),
                        type = WallpaperType.VIDEO.toProto(),
                        path = "/live/system.mp4",
                        service = WallpaperServiceType.VIDEO.toProto()
                    ),
                    LivePreference(
                        flag = WallpaperFlag.LOCK.toProto(),
                        type = WallpaperType.IMAGE.toProto(),
                        path = "/live/lock.png",
                        service = WallpaperServiceType.IMAGE.toProto()
                    )
                ),
                previewPreference = LivePreference(
                    type = WallpaperType.IMAGE.toProto(),
                    path = "/preview/new-image.png",
                    service = WallpaperServiceTypeProto.UNSPECIFIED
                )
            )
        }

        repository.promotePreviewSelectionToWallpaper(WallpaperFlag.SYSTEM)

        repository.getPreviewPath().shouldBeNull()
        repository.getWallpaperSelectionFlow(
            isPreview = false,
            serviceType = WallpaperServiceType.IMAGE,
            wallpaperFlag = WallpaperFlag.SYSTEM
        ).test {
            awaitItem() shouldBe WallpaperSelection(
                flag = WallpaperFlag.SYSTEM,
                path = "/preview/new-image.png",
                type = WallpaperType.IMAGE,
                service = WallpaperServiceType.IMAGE
            )
            cancelAndIgnoreRemainingEvents()
        }
        repository.getWallpaperSelectionFlow(
            isPreview = false,
            serviceType = WallpaperServiceType.IMAGE,
            wallpaperFlag = WallpaperFlag.LOCK
        ).test {
            awaitItem() shouldBe WallpaperSelection(
                flag = WallpaperFlag.LOCK,
                path = "/live/lock.png",
                type = WallpaperType.IMAGE,
                service = WallpaperServiceType.IMAGE
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `promotePreviewSelectionToWallpaper is a no-op when preview is empty`() = runTest {
        val dataStore = createDataStore("promote_preview_noop.preferences_pb", backgroundScope)
        val repository = UserPreferencesRepository(dataStore)

        dataStore.updateData {
            it.copy(
                livePreference = listOf(
                    LivePreference(
                        flag = WallpaperFlag.LOCK.toProto(),
                        type = WallpaperType.VIDEO.toProto(),
                        path = "/live/lock.mp4",
                        service = WallpaperServiceType.VIDEO.toProto()
                    )
                )
            )
        }

        repository.promotePreviewSelectionToWallpaper(WallpaperFlag.SYSTEM)

        repository.getPreviewPath().shouldBeNull()
        repository.getWallpaperSelectionFlow(
            isPreview = false,
            serviceType = WallpaperServiceType.VIDEO,
            wallpaperFlag = WallpaperFlag.LOCK
        ).test {
            awaitItem() shouldBe WallpaperSelection(
                flag = WallpaperFlag.LOCK,
                path = "/live/lock.mp4",
                type = WallpaperType.VIDEO,
                service = WallpaperServiceType.VIDEO
            )
            cancelAndIgnoreRemainingEvents()
        }
        repository.getWallpaperSelectionFlow(
            isPreview = false,
            serviceType = WallpaperServiceType.VIDEO,
            wallpaperFlag = WallpaperFlag.SYSTEM
        ).test {
            awaitItem().shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearPreviewData removes preview selection`() = runTest {
        val repository = createRepository("clear_preview.preferences_pb", backgroundScope)

        repository.setPreviewWallpaperType(WallpaperType.IMAGE)
        repository.setPreviewWallpaperService(WallpaperServiceType.IMAGE)
        repository.setPreviewPath("/preview/image.png")

        repository.clearPreviewData()

        repository.getPreviewPath().shouldBeNull()
        repository.getWallpaperSelectionFlow(
            isPreview = true,
            serviceType = WallpaperServiceType.IMAGE,
            wallpaperFlag = WallpaperFlag.SYSTEM
        ).test {
            awaitItem().shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createRepository(
        fileName: String,
        scope: CoroutineScope
    ): UserPreferencesRepository = UserPreferencesRepository(createDataStore(fileName, scope))

    private fun createDataStore(
        fileName: String,
        scope: CoroutineScope
    ): DataStore<UserPreferencesData> = DataStoreFactory.create(
        serializer = UserPreferencesSerializer(ProtoBuf.Default),
        scope = scope,
        produceFile = { tempDir.resolve(fileName).toFile() }
    )
}



