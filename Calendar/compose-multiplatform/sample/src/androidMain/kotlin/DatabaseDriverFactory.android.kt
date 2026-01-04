package com.kizitonwose.calendar.compose.multiplatform.sample

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.kizitonwose.calendar.sample.db.RemindrDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(RemindrDatabase.Schema, context, "remindr_v3.db")
    }
}
