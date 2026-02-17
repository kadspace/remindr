package com.remindr.app.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class AiService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    suspend fun parseItem(
        input: String,
        apiKey: String,
        currentDate: LocalDate,
    ): ParsedItem? {
        if (apiKey.isBlank()) throw IllegalArgumentException("API Key is empty")

        val systemPrompt = """
            You are Remindr's AI assistant. Parse user input into a reminder.

            Current Date: $currentDate (YYYY-MM-DD).
            Current Year: ${currentDate.year}.

            CRITICAL INSTRUCTION: Calculate the exact target date based on "Today".
            - If "tomorrow" is Jan 1st and today is Dec 31st, the YEAR must be ${currentDate.year + 1}.
            - If the event is in "January" and today is "December", increment the year.

            RECURRENCE & DATE RULES:
            - "first of every month" or "1st of each month" -> set day=1, recurrence_type=MONTHLY
            - "discover payment 1st of every month" -> title like "Discover payment", day=1, recurrence_type=MONTHLY
            - "every Monday" -> find next Monday's date, recurrence_type=WEEKLY
            - "every month" or "monthly" -> recurrence_type=MONTHLY, keep the day from context; if day is unclear use day=1
            - "every year" or "annually" or "yearly" -> recurrence_type=YEARLY
            - "every day" or "daily" -> recurrence_type=DAILY
            - "biweekly" or "every two weeks" -> recurrence_type=WEEKLY (note: biweekly)
            - If the user says "on the 15th" with "monthly" -> day=15, recurrence_type=MONTHLY
            - For recurring items, set the NEXT occurrence as the date

            Extract the EVENT DATE from the text.
            If no date is mentioned, use Today's date. If no time context at all (just a note/thought), set year/month/day to null.

            EXTRACT:
            - title: short summary
            - description: full details (or null)
            - date/time (if mentioned, null if not)
            - amount: dollar amount if mentioned (e.g., "${'$'}1400" -> 1400.0)
            - recurrence_type: DAILY | WEEKLY | MONTHLY | YEARLY (if recurring)
            - recurrence_end_year, recurrence_end_month: if end date mentioned
            - reminder_offsets: list of minutes before event to remind (default [0])
            - color_index: always 0

            Output ONLY valid JSON:
            {
                "title": "Short title",
                "description": "Full details or null",
                "year": 2025, "month": 1, "day": 1,
                "end_year": null, "end_month": null, "end_day": null,
                "hour": 8, "minute": 0,
                "color_index": 0,
                "amount": null,
                "recurrence_type": null,
                "recurrence_end_year": null, "recurrence_end_month": null,
                "nag_enabled": false,
                "reminder_offsets": [0]
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
                        Message(role = "user", content = input),
                    ),
                    response_format = ResponseFormat(type = "json_object"),
                )
            )
        }.body()

        if (response.error != null) {
            throw Exception("Groq API Error: ${response.error.message} (Type: ${response.error.type})")
        }

        val jsonString = response.choices?.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Empty response from AI (No choices)")

        return Json { ignoreUnknownKeys = true }.decodeFromString<ParsedItem>(jsonString)
    }
}
