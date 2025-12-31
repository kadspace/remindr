package com.kizitonwose.calendar.compose.multiplatform.sample

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
