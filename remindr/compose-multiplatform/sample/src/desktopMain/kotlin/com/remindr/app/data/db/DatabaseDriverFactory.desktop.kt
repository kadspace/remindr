package com.remindr.app.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.remindr.app.db.RemindrDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:remindr_v4.db")
        if (!File("remindr_v4.db").exists()) {
            RemindrDatabase.Schema.create(driver)
        }
        return driver
    }
}
