package com.graytsar.livewallpaper.domain

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import com.graytsar.livewallpaper.util.Util
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.nio.file.Path

class ImportMediaUseCaseTest {

    private val context = mockk<Context>()
    private val contentResolver = mockk<ContentResolver>()
    private val uri = mockk<Uri>()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `invoke imports image into image directory and closes stream`() = runTest {
        val filesDir = tempDir.toFile()
        val input = CloseTrackingInputStream("image-bytes".toByteArray())
        every { context.filesDir } returns filesDir
        every { contentResolver.openInputStream(uri) } returns input

        val importedFile = requireNotNull(ImportMediaUseCase(context, contentResolver)(uri, WallpaperType.IMAGE))

        importedFile.parentFile shouldBe Util.getImageImportDirectory(context)
        importedFile.name.startsWith("image_") shouldBe true
        importedFile.readText() shouldBe "image-bytes"
        input.closed shouldBe true
        verify(exactly = 1) { contentResolver.openInputStream(uri) }
    }

    @Test
    fun `invoke imports video into video directory and closes stream`() = runTest {
        val filesDir = tempDir.toFile()
        val input = CloseTrackingInputStream("video-bytes".toByteArray())
        every { context.filesDir } returns filesDir
        every { contentResolver.openInputStream(uri) } returns input

        val importedFile = requireNotNull(ImportMediaUseCase(context, contentResolver)(uri, WallpaperType.VIDEO))

        importedFile.parentFile shouldBe Util.getVideoImportDirectory(context)
        importedFile.name.startsWith("video_") shouldBe true
        importedFile.readText() shouldBe "video-bytes"
        input.closed shouldBe true
        verify(exactly = 1) { contentResolver.openInputStream(uri) }
    }

    @Test
    fun `invoke returns null when resolver cannot open input stream`() = runTest {
        every { contentResolver.openInputStream(uri) } returns null

        val result = ImportMediaUseCase(context, contentResolver)(uri, WallpaperType.IMAGE)

        result.shouldBeNull()
        verify(exactly = 1) { contentResolver.openInputStream(uri) }
    }

    @Test
    fun `invoke returns null for wallpaper type none without opening stream`() = runTest {
        val result = ImportMediaUseCase(context, contentResolver)(uri, WallpaperType.NONE)

        result.shouldBeNull()
        verify(exactly = 0) { contentResolver.openInputStream(any()) }
    }

    private class CloseTrackingInputStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
        var closed: Boolean = false
            private set

        override fun close() {
            closed = true
            super.close()
        }
    }
}


