package com.kizitonwose.calendar.compose.multiplatform.sample

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AIService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    suspend fun parseNote(input: String, apiKey: String): ParsedNote? {
        if (apiKey.isBlank()) throw IllegalArgumentException("API Key is empty")
        
        val systemPrompt = """
            You are a helper that extracts calendar event details from text.
            Evaluate the current year as 2025.
            Output ONLY valid JSON. No markdown, no backticks, no explanations.
            JSON Schema:
            {
               "text": "The main content of the note",
               "hour": 8 (int, 0-23, default 8),
               "minute": 0 (int, 0-59, default 0),
               "day_offset": 0 (int, 0=today, 1=tomorrow, etc, default 0),
               "color_index": 0 (int, 0-7, default 0)
            }
        """.trimIndent()

        val response: ChatCompletionResponse = client.post("https://api.groq.com/openai/v1/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(
                ChatCompletionRequest(
                    model = "llama-3.1-8b-instant",
                    messages = listOf(
                        Message(role = "system", content = systemPrompt),
                        Message(role = "user", content = input)
                    ),
                    response_format = ResponseFormat(type = "json_object")
                )
            )
        }.body()

        if (response.error != null) {
            throw Exception("Groq API Error: ${response.error.message} (Type: ${response.error.type})")
        }

        val jsonString = response.choices?.firstOrNull()?.message?.content
             ?: throw IllegalStateException("Empty response from AI (No choices)")
        
        println("AIService Raw JSON: $jsonString") // LOGGING

        return Json { ignoreUnknownKeys = true }.decodeFromString<ParsedNote>(jsonString)
    }
}

@Serializable
data class ParsedNote(
    val text: String,
    val hour: Int = 8,
    val minute: Int = 0,
    @SerialName("day_offset") val dayOffset: Int = 0,
    @SerialName("color_index") val colorIndex: Int = 0
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val response_format: ResponseFormat? = null,
    val temperature: Double = 0.5
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice>? = null,
    val error: ErrorDetails? = null
)

@Serializable
data class ErrorDetails(
    val message: String,
    val type: String? = null,
    val code: String? = null
)

@Serializable
data class Choice(
    val message: Message
)
