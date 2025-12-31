package com.kizitonwose.calendar.compose.multiplatform.sample

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GeminiService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val apiKey = "" // User needs to provide this

    suspend fun parseNote(input: String, apiKey: String): ParsedNote? {
        if (apiKey.isBlank()) throw IllegalArgumentException("API Key is empty")
        
        if (input.trim() == "debug:models") {
             val models = listModels(apiKey)
             throw Exception("Available Models:\n$models")
        }
        
        val prompt = """
            Extract the following details from this text: "$input"
            return ONLY a JSON object with these fields:
            - "text": The main content of the note.
            - "hour": The hour (0-23) inferred. Default to 8 if not found.
            - "minute": The minute (0-59). Default to 0.
            - "day_offset": 0 for today/now, 1 for tomorrow, etc. Default 0.
            - "color_index": A number 0-7 representing a color (0=Blue, 1=Red, 2=Brown, 3=Grey, 4=Teal, 5=Cyan, 6=Pink, 7=Orange). Guess based on text keywords (e.g. "red" -> 1, "urgent" -> 1). Default 0.
        """.trimIndent()

        val response: GeminiResponse = client.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite-001:generateContent?key=$apiKey") {
            contentType(ContentType.Application.Json)
            setBody(GeminiRequest(contents = listOf(Content(parts = listOf(Part(text = prompt))))))
        }.body()

        if (response.error != null) {
             throw Exception("API Error: ${response.error.message} (Code: ${response.error.code})")
        }

        val jsonString = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        val cleanJson = jsonString?.replace("```json", "")?.replace("```", "")?.trim()
            ?: throw IllegalStateException("Empty response from AI (No candidates)")

        return Json { ignoreUnknownKeys = true }.decodeFromString<ParsedNote>(cleanJson)
    }

    private suspend fun listModels(apiKey: String): String {
        try {
            val response: ModelListResponse = client.get("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey") {
                contentType(ContentType.Application.Json)
            }.body()
            return response.models?.joinToString("\n") { it.name.replace("models/", "") } ?: "No models found."
        } catch (e: Exception) {
            return "Failed to list models: ${e.message}"
        }
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
data class GeminiRequest(val contents: List<Content>)

@Serializable
data class Content(val parts: List<Part>)

@Serializable
data class Part(val text: String)

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

@Serializable
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

@Serializable
data class Candidate(val content: Content?)

@Serializable
data class ModelListResponse(val models: List<Model>?)

@Serializable
data class Model(val name: String)
