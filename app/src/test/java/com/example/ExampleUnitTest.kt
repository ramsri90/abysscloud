package com.example

import com.example.ui.MAX_MEDIA_SIZE_BYTES
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun maxMediaSize_isFiftyMegabytes() {
        val expectedBytes = 50L * 1024L * 1024L
        assertEquals(52428800L, expectedBytes)
        assertEquals(expectedBytes, MAX_MEDIA_SIZE_BYTES)
    }

    @Test
    fun mediaValidation_acceptsImagesAndVideosBelowFiftyMb() {
        val validImageMime = "image/jpeg"
        val validVideoMime = "video/mp4"
        val invalidMime = "application/pdf"
        val validSize = 25L * 1024L * 1024L
        val oversizedSize = 51L * 1024L * 1024L

        val isImageOrVideoValid = { mime: String, size: Long ->
            (mime.startsWith("image/") || mime.startsWith("video/")) && size in 1..MAX_MEDIA_SIZE_BYTES
        }

        assertTrue(isImageOrVideoValid(validImageMime, validSize))
        assertTrue(isImageOrVideoValid(validVideoMime, validSize))
        assertFalse(isImageOrVideoValid(invalidMime, validSize))
        assertFalse(isImageOrVideoValid(validImageMime, oversizedSize))
        assertFalse(isImageOrVideoValid(validVideoMime, oversizedSize))
    }
}
