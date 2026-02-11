package com.remindr.app.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate

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
        existingGroupNames: List<String> = emptyList(),
    ): ParsedItem? {
        if (apiKey.isBlank()) throw IllegalArgumentException("API Key is empty")

        val groupsJson = if (existingGroupNames.isNotEmpty()) {
            existingGroupNames.joinToString(", ") { "\"$it\"" }
        } else {
            "[]"
        }

        val systemPrompt = """
            You are Remindr's AI assistant. Parse user input into structured item data.

            Current Date: $currentDate (YYYY-MM-DD).
            Current Year: ${currentDate.year}.
            Existing Groups: [$groupsJson]

            CRITICAL INSTRUCTION: Calculate the exact target date based on "Today".
            - If "tomorrow" is Jan 1st and today is Dec 31st, the YEAR must be ${currentDate.year + 1}.
            - If the event is in "January" and today is "December", increment the year.

            RECURRENCE & DATE RULES:
            - "first of every month" or "1st of each month" → set day=1, recurrence_type=MONTHLY
            - "every Monday" → find next Monday's date, recurrence_type=WEEKLY
            - "every month" or "monthly" → recurrence_type=MONTHLY, keep the day from context or use day=1
            - "every year" or "annually" or "yearly" → recurrence_type=YEARLY
            - "every day" or "daily" → recurrence_type=DAILY
            - "biweekly" or "every two weeks" → recurrence_type=WEEKLY (note: biweekly)
            - If the user says "on the 15th" with "monthly" → day=15, recurrence_type=MONTHLY
            - For recurring items, set the NEXT occurrence as the date (if "first of every month" and today is Feb 11, set to Mar 1)

            Extract the EVENT DATE from the text.
            If no date is mentioned, use Today's date. If no time context at all (just a note/thought), set year/month/day to null.

            EXTRACT:
            - title: short summary
            - description: full details
            - date/time (if mentioned, null if not)
            - type: TASK | BILL | GOAL | NOTE | RESEARCH
            - severity: HIGH (consequences if missed) | MEDIUM (regular)
            - amount: dollar amount if mentioned (e.g., "${'$'}1400" -> 1400.0)
            - recurrence_type: DAILY | WEEKLY | MONTHLY | YEARLY (if recurring)
            - recurrence_end_year, recurrence_end_month: if end date mentioned (e.g., "ending 03/26")
            - group_name: assign to existing group OR suggest new group name
            - is_new_group: true if suggesting a new group that doesn't exist yet
            - parent_hint: if this seems like a sub-task of something, mention what

            TYPE CLASSIFICATION:
            - BILL: payments, rent, subscriptions, financial obligations
            - TASK: todos, errands, things to do
            - GOAL: long-term objectives, aspirations
            - NOTE: thoughts, ideas, things to remember
            - RESEARCH: things to look up, investigate

            SEVERITY CLASSIFICATION:
            Use "HIGH" for: Bills, payments, appointments, birthdays, deadlines, anything with consequences
            Use "MEDIUM" for: Shopping lists, notes, ideas, general todos

            COLOR MAPPING (indices):
            0: Red/Copper (urgent, bills)
            1: Coral (social, events)
            2: Teal/Green (health, wellness)
            3: Sage/Beige (personal, misc)
            4: Dark Grey (work, formal)

            Output ONLY valid JSON:
            {
                "title": "Short title",
                "description": "Full details or null",
                "year": 2025, "month": 1, "day": 1,
                "end_year": null, "end_month": null, "end_day": null,
                "hour": 8, "minute": 0,
                "color_index": 0,
                "severity": "HIGH or MEDIUM",
                "type": "TASK",
                "amount": null,
                "recurrence_type": null,
                "recurrence_end_year": null, "recurrence_end_month": null,
                "group_name": "Group Name or null",
                "is_new_group": false,
                "parent_hint": null,
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
