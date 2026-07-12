package com.example.ds9reader.di

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
    single {
        AndroidSqliteDriver(
            schema = LibraryDatabase.Schema,
            context = androidContext(),
            name = "ds9_reader.db",
        )
    }
    single<BookStorage> { AndroidBookStorage(androidContext()) }
    single<FilePicker> { AndroidFilePicker(androidContext()) }
}
