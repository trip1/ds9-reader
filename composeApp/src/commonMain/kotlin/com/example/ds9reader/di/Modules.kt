package com.example.ds9reader.di

import app.cash.sqldelight.db.SqlDriver
import com.example.ds9reader.data.BookImporter
import com.example.ds9reader.data.SqlLibraryRepository
import com.example.ds9reader.database.LibraryDatabase
import com.example.ds9reader.domain.FilePicker
import com.example.ds9reader.domain.ImportEpubsUseCase
import com.example.ds9reader.domain.LibraryPathProvider
import com.example.ds9reader.domain.LibraryRepository
import com.example.ds9reader.platform.PlatformFilePicker
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformModule(): Module

val appModule = module {
    single { LibraryDatabase(get<SqlDriver>()) }
    single<LibraryRepository> { SqlLibraryRepository(get()) }
    single<ImportEpubsUseCase> { ImportEpubsUseCase(get(), get()) }
    single<FilePicker> { PlatformFilePicker() }
}
