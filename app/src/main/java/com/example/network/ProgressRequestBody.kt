package com.example.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import java.io.InputStream

class ProgressRequestBody(
    private val contentType: MediaType?,
    private val contentLength: Long,
    private val onBytesProgress: (bytesWritten: Long, totalBytes: Long, progress: Int) -> Unit,
    private val openInputStream: () -> InputStream?
) : RequestBody() {

    // Overload for legacy single-int progress callback
    constructor(
        contentType: MediaType?,
        contentLength: Long,
        onProgress: (progress: Int) -> Unit,
        openInputStream: () -> InputStream?
    ) : this(
        contentType = contentType,
        contentLength = contentLength,
        onBytesProgress = { _, _, p -> onProgress(p) },
        openInputStream = openInputStream
    )

    override fun contentType(): MediaType? = contentType

    // Ensure we return the exact length so OkHttp doesn't log standard chunked warnings
    override fun contentLength(): Long = contentLength

    override fun writeTo(sink: BufferedSink) {
        // When OkHttp's MultipartBody calculates content length, sink is an okio.Buffer.
        // During that measurement phase, do NOT fire live progress events.
        val isMeasurementPhase = sink is Buffer

        val stream = openInputStream() ?: throw java.io.IOException("Unable to open input stream")
        // Use 64 KB buffer for high-throughput streaming instead of tiny 4 KB
        val buffer = ByteArray(64 * 1024)
        var totalBytesRead: Long = 0
        var bytesRead: Int
        var lastReportedTime = 0L

        stream.use { s ->
            while (s.read(buffer).also { bytesRead = it } != -1) {
                val bytesToWrite = if (contentLength > 0 && totalBytesRead + bytesRead > contentLength) {
                    (contentLength - totalBytesRead).toInt().coerceAtLeast(0)
                } else {
                    bytesRead
                }

                if (bytesToWrite > 0) {
                    sink.write(buffer, 0, bytesToWrite)
                    totalBytesRead += bytesToWrite
                }

                if (!isMeasurementPhase) {
                    val progress = if (contentLength > 0) {
                        ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }
                    val now = System.currentTimeMillis()
                    // Report progress smoothly at ~100ms intervals, on first byte, or at 100%
                    if (now - lastReportedTime >= 100 || progress == 100 || totalBytesRead == contentLength) {
                        lastReportedTime = now
                        onBytesProgress(totalBytesRead, contentLength, progress)
                    }
                }

                if (contentLength > 0 && totalBytesRead >= contentLength) {
                    break
                }
            }
            sink.flush()
            if (!isMeasurementPhase && contentLength > 0) {
                onBytesProgress(totalBytesRead, contentLength, 100)
            }
        }
    }
}

