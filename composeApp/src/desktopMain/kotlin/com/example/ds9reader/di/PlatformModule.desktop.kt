package com.example.ds9reader.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.ds9reader.database.LibraryDatabase
import com.example.ds9reader.domain.BookStorage
import com.example.ds9reader.domain.FilePicker
import com.example.ds9reader.platform.DesktopFilePicker
import com.example.ds9reader.platform.JvmBookStorage
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual fun platformModule(): Module = module {
    single<SqlDriver> {
        val dbFile = File(System.getProperty("user.home"), ".ds9-reader/library.db")
        dbFile.parentFile?.mkdirs()
        val needsCreate = !dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (needsCreate) {
            LibraryDatabase.Schema.create(driver)
        }
        driver
    }
    single<BookStorage> {
        JvmBookStorage(File(System.getProperty("user.home"), ".ds9-reader/books"))
    }
    single<FilePicker> { DesktopFilePicker() }
}
