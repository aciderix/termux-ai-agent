package com.example

import com.example.network.ChatRequest
import com.example.network.PromptObject
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Test
import java.io.File

class MoshiTest {
    @Test
    fun testMoshi() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(ChatRequest::class.java)
        val req = ChatRequest(model = "qwen3.7-plus", promptObject = PromptObject("Test"))
        val json = adapter.toJson(req)
        File("test_output.json").writeText(json)
    }
}
