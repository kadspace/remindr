package com.kizitonwose.remindr.compose.multiplatform.sample

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kizitonwose.remindr.sample.db.RemindrDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:remindr.db")
        if (!File("remindr.db").exists()) {
            RemindrDatabase.Schema.create(driver)
        }
        return driver
    }
}
