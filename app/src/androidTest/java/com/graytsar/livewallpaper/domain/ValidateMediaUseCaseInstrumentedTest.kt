package com.graytsar.livewallpaper.domain

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File

class ValidateMediaUseCaseInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val contentResolver = context.contentResolver
    private val useCase = ValidateMediaUseCase(context, contentResolver)
    private val createdFiles = mutableListOf<File>()

    @AfterEach
    fun tearDown() {
        createdFiles.forEach { it.delete() }
        createdFiles.clear()
    }

    @Test
    fun validateImageReturnsTrueForGeneratedPng() {
        runBlocking {
            val file = createPngFile()

            useCase.validateImage(Uri.fromFile(file)) shouldBe true
        }
    }

    @Test
    fun validateImageReturnsFalseForPlainTextFile() {
        runBlocking {
            val file = createTextFile("invalid-image.txt", "not-an-image")

            useCase.validateImage(Uri.fromFile(file)) shouldBe false
        }
    }

    @Test
    fun validateVideoReturnsFalseForNonVideoFile() {
        runBlocking {
            val file = createTextFile("not-video.txt", "still-not-a-video")

            useCase.validateVideo(Uri.fromFile(file)) shouldBe false
        }
    }

    @Test
    fun validateImageReturnsFalseForMissingFile() {
        runBlocking {
            val missingFile = File(context.cacheDir, "missing-image.png").apply { delete() }

            useCase.validateImage(Uri.fromFile(missingFile)) shouldBe false
        }
    }

    @Test
    fun validateVideoReturnsFalseForMissingFile() {
        runBlocking {
            val missingFile = File(context.cacheDir, "missing-video.mp4").apply { delete() }

            useCase.validateVideo(Uri.fromFile(missingFile)) shouldBe false
        }
    }

    private fun createPngFile(): File {
        val file = createTrackedFile("valid-image.png")
        Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).useBitmap { bitmap ->
            file.outputStream().use { output ->
                bitmap.eraseColor(0xFF336699.toInt())
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }
        return file
    }

    private fun createTextFile(name: String, content: String): File {
        val file = createTrackedFile(name)
        file.writeText(content)
        return file
    }

    private fun createTrackedFile(name: String): File {
        return File(context.cacheDir, name).apply {
            parentFile?.mkdirs()
            delete()
            createNewFile()
            createdFiles += this
        }
    }

    private inline fun Bitmap.useBitmap(block: (Bitmap) -> Unit) {
        try {
            block(this)
        } finally {
            recycle()
        }
    }
}





