package com.example.ds9reader.di

import app.cash.sqldelight.db.SqlDriver
import com.example.ds9reader.calibre.CalibreClient
import com.example.ds9reader.data.SqlLibraryRepository
import com.example.ds9reader.database.LibraryDatabase
import com.example.ds9reader.domain.BookStorage
import com.example.ds9reader.domain.FilePicker
import com.example.ds9reader.domain.LibraryRepository
import com.example.ds9reader.domain.OpenBookUseCase
import com.example.ds9reader.domain.SyncWithCalibreUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformModule(): Module

val appModule = module {
    single { LibraryDatabase(get<SqlDriver>()) }
    single<LibraryRepository> { SqlLibraryRepository(get()) }
    single { CalibreClient() }
    single { SyncWithCalibreUseCase(get(), get(), get()) }
    single { OpenBookUseCase(get(), get()) }
}
