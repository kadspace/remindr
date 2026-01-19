package com.kizitonwose.remindr.compose.multiplatform.sample

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
