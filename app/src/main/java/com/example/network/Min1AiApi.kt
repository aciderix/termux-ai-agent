package com.example.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class ChatRequest(
    val type: String = "UNIFY_CHAT_WITH_AI",
    val model: String,
    val promptObject: PromptObject
)

@JsonClass(generateAdapter = true)
data class PromptObject(
    val prompt: String
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    val type: String?,
    val model: String?,
    val text: String?
)

interface Min1AiApi {
    @POST("api/chat-with-ai")
    suspend fun chat(
        @Header("API-KEY") apiKey: String,
        @Body request: ChatRequest
    ): ChatResponse
}
