package com.remindr.app.data.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParsedItem(
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
    val amount: Double? = null,
    @SerialName("recurrence_type") val recurrenceType: String? = null,
    @SerialName("recurrence_end_year") val recurrenceEndYear: Int? = null,
    @SerialName("recurrence_end_month") val recurrenceEndMonth: Int? = null,
    @SerialName("nag_enabled") val nagEnabled: Boolean = false,
    @SerialName("reminder_offsets") val reminderOffsets: List<Long> = emptyList(),
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val response_format: ResponseFormat? = null,
    val temperature: Double = 0.5,
)

@Serializable
data class Message(
    val role: String,
    val content: String,
)

@Serializable
data class ResponseFormat(
    val type: String,
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice>? = null,
    val error: ErrorDetails? = null,
)

@Serializable
data class ErrorDetails(
    val message: String,
    val type: String? = null,
    val code: String? = null,
)

@Serializable
data class Choice(
    val message: Message,
)
