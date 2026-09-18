package com.example.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface TelegramBotService {
    @Multipart
    @POST("/bot{token}/sendDocument")
    suspend fun sendDocument(
        @Path("token") token: String,
        @Part("chat_id") chatId: RequestBody,
        @Part document: MultipartBody.Part,
        @Part("caption") caption: RequestBody? = null
    ): Response<TelegramResponse<TelegramMessage>>

    @Multipart
    @POST("/bot{token}/sendPhoto")
    suspend fun sendPhoto(
        @Path("token") token: String,
        @Part("chat_id") chatId: RequestBody,
        @Part photo: MultipartBody.Part,
        @Part("caption") caption: RequestBody? = null
    ): Response<TelegramResponse<TelegramMessage>>

    @Multipart
    @POST("/bot{token}/sendVideo")
    suspend fun sendVideo(
        @Path("token") token: String,
        @Part("chat_id") chatId: RequestBody,
        @Part video: MultipartBody.Part,
        @Part("caption") caption: RequestBody? = null,
        @Part("supports_streaming") supportsStreaming: RequestBody? = null
    ): Response<TelegramResponse<TelegramMessage>>

    @GET("/bot{token}/getFile")
    suspend fun getFile(
        @Path("token") token: String,
        @Query("file_id") fileId: String
    ): Response<TelegramResponse<TelegramFile>>

    @GET
    @Streaming
    suspend fun downloadFile(
        @Url url: String
    ): Response<okhttp3.ResponseBody>

    @POST("/bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token") token: String,
        @Query("chat_id") chatId: String,
        @Query("text") text: String
    ): Response<TelegramResponse<TelegramMessage>>

    @GET("/bot{token}/getUpdates")
    suspend fun getUpdates(
        @Path("token") token: String,
        @Query("offset") offset: Long? = null,
        @Query("limit") limit: Int? = 100,
        @Query("timeout") timeout: Int? = 0
    ): Response<TelegramResponse<List<TelegramUpdate>>>

    @GET("/bot{token}/getMe")
    suspend fun getMe(
        @Path("token") token: String
    ): Response<TelegramResponse<TelegramBotUser>>

    @POST("/bot{token}/deleteWebhook")
    suspend fun deleteWebhook(
        @Path("token") token: String
    ): Response<TelegramResponse<Boolean>>
}

