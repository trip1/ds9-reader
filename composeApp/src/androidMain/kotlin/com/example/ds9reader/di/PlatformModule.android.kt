package com.example.ds9reader.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.ds9reader.database.LibraryDatabase
import com.example.ds9reader.domain.BookStorage
import com.example.ds9reader.domain.FilePicker
import com.example.ds9reader.platform.AndroidBookStorage
import com.example.ds9reader.platform.AndroidFilePicker
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<SqlDriver> {
        // Fresh DB filename so older installs with incomplete schema don't crash on launch.
        // (AppSetting was added without a SQLDelight migration version bump.)
        AndroidSqliteDriver(
            schema = LibraryDatabase.Schema,
            context = androidContext(),
            name = "ds9_reader_v2.db",
        )
    }
    single<BookStorage> { AndroidBookStorage(androidContext()) }
    single<FilePicker> { AndroidFilePicker(androidContext()) }
}
