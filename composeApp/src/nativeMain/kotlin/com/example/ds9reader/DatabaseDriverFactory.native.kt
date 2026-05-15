package com.example.ds9reader

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.ds9reader.database.LibraryDatabase

actual fun createSqlDriver(): SqlDriver {
    return NativeSqliteDriver(
        schema = LibraryDatabase.Schema,
        name = "library.db"
    )
}
