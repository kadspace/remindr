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
import kotlinx.datetime.LocalDate

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

    suspend fun parseNote(input: String, apiKey: String, currentDate: LocalDate): ParsedNote? {
        if (apiKey.isBlank()) throw IllegalArgumentException("API Key is empty")
                val systemPrompt = """
                You are a helper that extracts calendar event details from text.
                Current Date: $currentDate (YYYY-MM-DD).
                Current Year: ${currentDate.year}.
                
                CRITICAL INSTRUCTION: Calculate the exact target date based on "Today".
                - If "tomorrow" is Jan 1st and today is Dec 31st, the YEAR must be ${currentDate.year + 1}.
                - If the event is in "January" and today is "December", increment the year.
                
                Extract the EVENT DATE from the text.
                If no date is mentioned, use Today's date.
                
                Output ONLY valid JSON.
                JSON Schema:
                {
                   "title": "Short title (e.g. 'Dentist')",
                   "description": "Full details (e.g. 'Bring insurance card, 123 Main St')",
                   "year": 2025,
                   "month": 1,
                   "day": 1,
                   "end_year": null (int, if range),
                   "end_month": null,
                   "end_day": null,
                   "hour": 8,
                   "minute": 0,
                   "color_index": 0,
                   "severity": "MEDIUM",
                   "recurrence_type": null,
                   "nag_enabled": false,
                   "reminder_offsets": [0]
                }
                
                COLOR MAPPING (Use these indices if a color is mentioned):
                0: Blue
                1: Red
                2: Brown
                3: Grey
                4: Teal/Green
                5: Cyan
                6: Pink
                7: Orange
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
    val title: String,
    val description: String? = null,
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    @SerialName("end_year") val endYear: Int? = null,
    @SerialName("end_month") val endMonth: Int? = null,
    @SerialName("end_day") val endDay: Int? = null,
    val hour: Int = 8,
    val minute: Int = 0,
    @SerialName("color_index") val colorIndex: Int = 0,
    val severity: String? = null,
    @SerialName("recurrence_type") val recurrenceType: String? = null,
    @SerialName("nag_enabled") val nagEnabled: Boolean = false,
    @SerialName("reminder_offsets") val reminderOffsets: List<Long> = emptyList()
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
