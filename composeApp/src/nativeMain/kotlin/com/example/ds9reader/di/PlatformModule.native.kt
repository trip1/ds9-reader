package com.example.ds9reader.di

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.ds9reader.database.LibraryDatabase
import com.example.ds9reader.domain.BookStorage
import com.example.ds9reader.domain.FilePicker
import com.example.ds9reader.platform.IosBookStorage
import com.example.ds9reader.platform.IosFilePicker
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single {
        NativeSqliteDriver(LibraryDatabase.Schema, "ds9_reader.db")
    }
    single<BookStorage> { IosBookStorage() }
    single<FilePicker> { IosFilePicker() }
}
