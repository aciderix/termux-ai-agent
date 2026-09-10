package com.example.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class ChatRequest(
    val type: String,
    val model: String,
    val promptObject: PromptObject
)

@JsonClass(generateAdapter = true)
data class PromptObject(
    val prompt: String
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    val aiRecordDetail: AiRecordDetail?
)

@JsonClass(generateAdapter = true)
data class AiRecordDetail(
    val resultObject: List<String>?
)

interface Min1AiApi {
    @POST("api/chat-with-ai")
    suspend fun chat(
        @Header("API-KEY") apiKey: String,
        @Body request: ChatRequest
    ): ChatResponse
}
