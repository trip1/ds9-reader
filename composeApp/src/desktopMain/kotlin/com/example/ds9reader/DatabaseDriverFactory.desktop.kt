package com.example.ds9reader

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.ds9reader.database.LibraryDatabase

actual fun createSqlDriver(): SqlDriver {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    LibraryDatabase.Schema.create(driver)
    return driver
}
