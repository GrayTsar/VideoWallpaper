package com.graytsar.livewallpaper.ui

import android.net.Uri
import app.cash.turbine.test
import com.graytsar.livewallpaper.core.common.model.WallpaperFlag
import com.graytsar.livewallpaper.core.common.model.WallpaperServiceType
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import com.graytsar.livewallpaper.domain.CleanupMediaUseCase
import com.graytsar.livewallpaper.domain.ImportMediaUseCase
import com.graytsar.livewallpaper.domain.ValidateMediaUseCase
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelPickerTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private val validateMediaUseCase = mockk<ValidateMediaUseCase>()
    private val importMediaUseCase = mockk<ImportMediaUseCase>()
    private val cleanupMediaUseCase = mockk<CleanupMediaUseCase>()
    private val pickerSelectionStore = mockk<PickerSelectionStore>(relaxed = true)

    private val viewModel = ViewModelPicker(
        validateMediaUseCase = validateMediaUseCase,
        importMediaUseCase = importMediaUseCase,
        cleanupMediaUseCase = cleanupMediaUseCase,
        pickerSelectionStore = pickerSelectionStore
    )

    @Test
    fun `onMediaSelected emits launch event for valid image`() = runTest {
        val uri = mockk<Uri>()
        val importedFile = File("imported-image")
        coEvery { validateMediaUseCase.validateImage(uri) } returns true
        coEvery { importMediaUseCase(uri, WallpaperType.IMAGE) } returns importedFile
        coEvery { cleanupMediaUseCase(importedFile.path) } returns Unit

        viewModel.events.test {
            viewModel.onMediaSelected(uri, WallpaperType.IMAGE)
            advanceUntilIdle()

            awaitItem() shouldBe ViewModelPicker.PickerEvent.LaunchWallpaperService(
                WallpaperServiceType.IMAGE
            )

            coVerify(exactly = 1) { validateMediaUseCase.validateImage(uri) }
            coVerify(exactly = 1) { importMediaUseCase(uri, WallpaperType.IMAGE) }
            coVerify(exactly = 1) { cleanupMediaUseCase(importedFile.path) }
            coVerify(exactly = 1) { pickerSelectionStore.saveSelection(importedFile.path, WallpaperType.IMAGE) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onMediaSelected emits launch event for valid video`() = runTest {
        val uri = mockk<Uri>()
        val importedFile = File("imported-video")
        coEvery { validateMediaUseCase.validateVideo(uri) } returns true
        coEvery { importMediaUseCase(uri, WallpaperType.VIDEO) } returns importedFile
        coEvery { cleanupMediaUseCase(importedFile.path) } returns Unit

        viewModel.events.test {
            viewModel.onMediaSelected(uri, WallpaperType.VIDEO)
            advanceUntilIdle()

            awaitItem() shouldBe ViewModelPicker.PickerEvent.LaunchWallpaperService(
                WallpaperServiceType.VIDEO
            )

            coVerify(exactly = 1) { validateMediaUseCase.validateVideo(uri) }
            coVerify(exactly = 1) { importMediaUseCase(uri, WallpaperType.VIDEO) }
            coVerify(exactly = 1) { cleanupMediaUseCase(importedFile.path) }
            coVerify(exactly = 1) { pickerSelectionStore.saveSelection(importedFile.path, WallpaperType.VIDEO) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onMediaSelected emits invalid image error when validation fails`() = runTest {
        val uri = mockk<Uri>()
        coEvery { validateMediaUseCase.validateImage(uri) } returns false

        viewModel.events.test {
            viewModel.onMediaSelected(uri, WallpaperType.IMAGE)
            advanceUntilIdle()

            awaitItem() shouldBe ViewModelPicker.PickerEvent.Error(ViewModelPicker.PickerUiError.InvalidImage)

            coVerify(exactly = 1) { validateMediaUseCase.validateImage(uri) }
            coVerify(exactly = 0) { validateMediaUseCase.validateVideo(any()) }
            coVerify(exactly = 0) { importMediaUseCase(any(), any()) }
            coVerify(exactly = 0) { cleanupMediaUseCase(any()) }
            coVerify(exactly = 0) { pickerSelectionStore.saveSelection(any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onMediaSelected emits invalid video error when validation fails`() = runTest {
        val uri = mockk<Uri>()
        coEvery { validateMediaUseCase.validateVideo(uri) } returns false

        viewModel.events.test {
            viewModel.onMediaSelected(uri, WallpaperType.VIDEO)
            advanceUntilIdle()

            awaitItem() shouldBe ViewModelPicker.PickerEvent.Error(ViewModelPicker.PickerUiError.InvalidVideo)

            coVerify(exactly = 1) { validateMediaUseCase.validateVideo(uri) }
            coVerify(exactly = 0) { validateMediaUseCase.validateImage(any()) }
            coVerify(exactly = 0) { importMediaUseCase(any(), any()) }
            coVerify(exactly = 0) { cleanupMediaUseCase(any()) }
            coVerify(exactly = 0) { pickerSelectionStore.saveSelection(any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onMediaSelected emits import error when import returns null`() = runTest {
        val uri = mockk<Uri>()
        coEvery { validateMediaUseCase.validateImage(uri) } returns true
        coEvery { importMediaUseCase(uri, WallpaperType.IMAGE) } returns null

        viewModel.events.test {
            viewModel.onMediaSelected(uri, WallpaperType.IMAGE)
            advanceUntilIdle()

            awaitItem() shouldBe ViewModelPicker.PickerEvent.Error(ViewModelPicker.PickerUiError.Import)

            coVerify(exactly = 1) { validateMediaUseCase.validateImage(uri) }
            coVerify(exactly = 0) { validateMediaUseCase.validateVideo(any()) }
            coVerify(exactly = 1) { importMediaUseCase(uri, WallpaperType.IMAGE) }
            coVerify(exactly = 0) { cleanupMediaUseCase(any()) }
            coVerify(exactly = 0) { pickerSelectionStore.saveSelection(any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onMediaSelected emits import error when import throws`() = runTest {
        val uri = mockk<Uri>()
        coEvery { validateMediaUseCase.validateImage(uri) } returns true
        coEvery { importMediaUseCase(uri, WallpaperType.IMAGE) } throws IllegalStateException("import failed")

        viewModel.events.test {
            viewModel.onMediaSelected(uri, WallpaperType.IMAGE)
            advanceUntilIdle()

            awaitItem() shouldBe ViewModelPicker.PickerEvent.Error(ViewModelPicker.PickerUiError.Import)

            coVerify(exactly = 1) { validateMediaUseCase.validateImage(uri) }
            coVerify(exactly = 0) { validateMediaUseCase.validateVideo(any()) }
            coVerify(exactly = 1) { importMediaUseCase(uri, WallpaperType.IMAGE) }
            coVerify(exactly = 0) { cleanupMediaUseCase(any()) }
            coVerify(exactly = 0) { pickerSelectionStore.saveSelection(any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onMediaSelected emits import error for wallpaper type none`() = runTest {
        val uri = mockk<Uri>()

        viewModel.events.test {
            viewModel.onMediaSelected(uri, WallpaperType.NONE)
            advanceUntilIdle()

            awaitItem() shouldBe ViewModelPicker.PickerEvent.Error(ViewModelPicker.PickerUiError.Import)

            coVerify(exactly = 0) { validateMediaUseCase.validateImage(any()) }
            coVerify(exactly = 0) { validateMediaUseCase.validateVideo(any()) }
            coVerify(exactly = 0) { importMediaUseCase(any(), any()) }
            coVerify(exactly = 0) { cleanupMediaUseCase(any()) }
            coVerify(exactly = 0) { pickerSelectionStore.saveSelection(any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onWallpaperSetResult promotes preview when wallpaper set succeeds`() = runTest {
        viewModel.onWallpaperSetResult(success = true, flag = WallpaperFlag.LOCK)
        advanceUntilIdle()

        coVerify(exactly = 1) { pickerSelectionStore.promotePreviewSelectionToWallpaper(WallpaperFlag.LOCK) }
        coVerify(exactly = 0) { pickerSelectionStore.clearPreviewSelection() }
    }

    @Test
    fun `onWallpaperSetResult clears preview when wallpaper set fails`() = runTest {
        viewModel.onWallpaperSetResult(success = false, flag = WallpaperFlag.SYSTEM)
        advanceUntilIdle()

        coVerify(exactly = 1) { pickerSelectionStore.clearPreviewSelection() }
        coVerify(exactly = 0) { pickerSelectionStore.promotePreviewSelectionToWallpaper(any()) }
    }
}


