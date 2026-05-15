package com.example.ds9reader

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.ds9reader.database.LibraryDatabase

class AndroidDriverFactory(private val context: Context) {
    fun create(): SqlDriver {
        return AndroidSqliteDriver(
            schema = LibraryDatabase.Schema,
            context = context,
            name = "library.db"
        )
    }
}
