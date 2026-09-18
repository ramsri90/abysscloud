package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TelegramResponse<T>(
    @Json(name = "ok") val ok: Boolean,
    @Json(name = "result") val result: T?,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramUpdate(
    @Json(name = "update_id") val updateId: Long,
    @Json(name = "message") val message: TelegramMessage? = null,
    @Json(name = "channel_post") val channelPost: TelegramMessage? = null
)

@JsonClass(generateAdapter = true)
data class TelegramChat(
    @Json(name = "id") val id: Long,
    @Json(name = "type") val type: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "username") val username: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramMessage(
    @Json(name = "message_id") val messageId: Long,
    @Json(name = "date") val date: Long? = null,
    @Json(name = "chat") val chat: TelegramChat? = null,
    @Json(name = "document") val document: TelegramDocument? = null,
    @Json(name = "photo") val photo: List<TelegramPhotoSize>? = null,
    @Json(name = "video") val video: TelegramVideo? = null,
    @Json(name = "audio") val audio: TelegramAudio? = null,
    @Json(name = "voice") val voice: TelegramVoice? = null,
    @Json(name = "video_note") val videoNote: TelegramVideoNote? = null,
    @Json(name = "animation") val animation: TelegramAnimation? = null,
    @Json(name = "text") val text: String? = null,
    @Json(name = "caption") val caption: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramPhotoSize(
    @Json(name = "file_id") val fileId: String,
    @Json(name = "width") val width: Int,
    @Json(name = "height") val height: Int,
    @Json(name = "file_size") val fileSize: Long? = null
)

@JsonClass(generateAdapter = true)
data class TelegramDocument(
    @Json(name = "file_id") val fileId: String,
    @Json(name = "file_name") val fileName: String? = null,
    @Json(name = "mime_type") val mimeType: String? = null,
    @Json(name = "file_size") val fileSize: Long? = null
)

@JsonClass(generateAdapter = true)
data class TelegramFile(
    @Json(name = "file_id") val fileId: String,
    @Json(name = "file_size") val fileSize: Long? = null,
    @Json(name = "file_path") val filePath: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramVideo(
    @Json(name = "file_id") val fileId: String,
    @Json(name = "file_name") val fileName: String? = null,
    @Json(name = "mime_type") val mimeType: String? = null,
    @Json(name = "file_size") val fileSize: Long? = null
)

@JsonClass(generateAdapter = true)
data class TelegramAudio(
    @Json(name = "file_id") val fileId: String,
    @Json(name = "file_name") val fileName: String? = null,
    @Json(name = "mime_type") val mimeType: String? = null,
    @Json(name = "file_size") val fileSize: Long? = null
)

@JsonClass(generateAdapter = true)
data class TelegramVoice(
    @Json(name = "file_id") val fileId: String,
    @Json(name = "mime_type") val mimeType: String? = null,
    @Json(name = "file_size") val fileSize: Long? = null
)

@JsonClass(generateAdapter = true)
data class TelegramVideoNote(
    @Json(name = "file_id") val fileId: String,
    @Json(name = "file_size") val fileSize: Long? = null
)

@JsonClass(generateAdapter = true)
data class TelegramAnimation(
    @Json(name = "file_id") val fileId: String,
    @Json(name = "file_name") val fileName: String? = null,
    @Json(name = "mime_type") val mimeType: String? = null,
    @Json(name = "file_size") val fileSize: Long? = null
)

@JsonClass(generateAdapter = true)
data class TelegramBotUser(
    @Json(name = "id") val id: Long,
    @Json(name = "is_bot") val isBot: Boolean = true,
    @Json(name = "first_name") val firstName: String,
    @Json(name = "username") val username: String? = null
)

