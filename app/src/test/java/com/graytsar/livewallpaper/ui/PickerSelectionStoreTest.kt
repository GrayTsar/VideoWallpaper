package com.graytsar.livewallpaper.ui

import com.graytsar.livewallpaper.core.common.model.WallpaperFlag
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import com.graytsar.livewallpaper.core.repository.UserPreferencesRepository
import com.graytsar.livewallpaper.util.toServiceType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PickerSelectionStoreTest {

    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val store = PickerSelectionStore(userPreferencesRepository)

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `saveSelection stores preview metadata in repository`() = runTest {
        store.saveSelection(path = "/preview/image.png", type = WallpaperType.IMAGE)

        coVerifyOrder {
            userPreferencesRepository.setPreviewWallpaperType(WallpaperType.IMAGE)
            userPreferencesRepository.setPreviewWallpaperService(WallpaperType.IMAGE.toServiceType())
            userPreferencesRepository.setPreviewPath("/preview/image.png")
        }
    }

    @Test
    fun `promotePreviewSelectionToWallpaper delegates to repository`() = runTest {
        store.promotePreviewSelectionToWallpaper(WallpaperFlag.LOCK)

        coVerify(exactly = 1) {
            userPreferencesRepository.promotePreviewSelectionToWallpaper(WallpaperFlag.LOCK)
        }
    }

    @Test
    fun `clearPreviewSelection deletes preview file and clears preview data`() = runTest {
        val previewFile = tempDir.resolve("preview-video.mp4").toFile().apply {
            writeText("preview")
        }
        coEvery { userPreferencesRepository.getPreviewPath() } returns previewFile.path

        store.clearPreviewSelection()

        previewFile.exists() shouldBe false
        coVerifyOrder {
            userPreferencesRepository.getPreviewPath()
            userPreferencesRepository.clearPreviewData()
        }
    }

    @Test
    fun `clearPreviewSelection clears preview data when preview path is missing`() = runTest {
        coEvery { userPreferencesRepository.getPreviewPath() } returns null

        store.clearPreviewSelection()

        coVerifyOrder {
            userPreferencesRepository.getPreviewPath()
            userPreferencesRepository.clearPreviewData()
        }
    }

    @Test
    fun `clearPreviewSelection clears preview data when preview file no longer exists`() = runTest {
        val missingFile = tempDir.resolve("missing-preview.png").toFile()
        coEvery { userPreferencesRepository.getPreviewPath() } returns missingFile.path

        store.clearPreviewSelection()

        missingFile.exists() shouldBe false
        coVerifyOrder {
            userPreferencesRepository.getPreviewPath()
            userPreferencesRepository.clearPreviewData()
        }
    }
}


