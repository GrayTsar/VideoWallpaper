package com.graytsar.livewallpaper.domain

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import com.graytsar.livewallpaper.util.Util
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File

class ImportMediaUseCaseInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val useCase = ImportMediaUseCase(context, context.contentResolver)
    private val sourceFiles = mutableListOf<File>()

    @AfterEach
    fun tearDown() {
        sourceFiles.forEach { it.delete() }
        sourceFiles.clear()
        Util.getImageImportDirectory(context).deleteRecursively()
        Util.getVideoImportDirectory(context).deleteRecursively()
    }

    @Test
    fun importImageCopiesBytesIntoImageDirectory() {
        runBlocking {
            val sourceFile = createSourceFile("source-image.bin", "image-content")

            val importedFile = requireNotNull(useCase(Uri.fromFile(sourceFile), WallpaperType.IMAGE))

            importedFile.exists() shouldBe true
            importedFile.parentFile shouldBe Util.getImageImportDirectory(context)
            importedFile.name.startsWith("image_") shouldBe true
            importedFile.readText() shouldBe "image-content"
        }
    }

    @Test
    fun importVideoCopiesBytesIntoVideoDirectory() {
        runBlocking {
            val sourceFile = createSourceFile("source-video.bin", "video-content")

            val importedFile = requireNotNull(useCase(Uri.fromFile(sourceFile), WallpaperType.VIDEO))

            importedFile.exists() shouldBe true
            importedFile.parentFile shouldBe Util.getVideoImportDirectory(context)
            importedFile.name.startsWith("video_") shouldBe true
            importedFile.readText() shouldBe "video-content"
        }
    }

    @Test
    fun importNoneReturnsNullWithoutCreatingImportedFiles() {
        runBlocking {
            val sourceFile = createSourceFile("source-none.bin", "unused-content")

            val importedFile = useCase(Uri.fromFile(sourceFile), WallpaperType.NONE)

            importedFile.shouldBeNull()
            Util.getImageImportDirectory(context).listFiles().orEmpty().isEmpty() shouldBe true
            Util.getVideoImportDirectory(context).listFiles().orEmpty().isEmpty() shouldBe true
        }
    }

    private fun createSourceFile(name: String, content: String): File {
        return File(context.cacheDir, name).apply {
            writeText(content)
            sourceFiles += this
        }
    }
}

