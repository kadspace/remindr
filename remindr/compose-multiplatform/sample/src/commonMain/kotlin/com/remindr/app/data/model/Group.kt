package com.remindr.app.data.model

import kotlinx.datetime.LocalDateTime

data class Group(
    val id: Long = -1,
    val name: String,
    val icon: String? = null,
    val description: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
