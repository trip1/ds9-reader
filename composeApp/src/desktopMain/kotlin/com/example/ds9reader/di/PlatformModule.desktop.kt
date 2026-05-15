package com.example.ds9reader.di

import com.example.ds9reader.createSqlDriver
import com.example.ds9reader.data.BookImporter
import com.example.ds9reader.data.DesktopBookImporter
import com.example.ds9reader.domain.LibraryPathProvider
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual fun platformModule(): Module = module {
    single { createSqlDriver() }
    
    single<LibraryPathProvider> {
        object : LibraryPathProvider {
            override fun getLibraryPath(): String {
                val dir = File(System.getProperty("user.home"), ".ds9-reader/library")
                if (!dir.exists()) dir.mkdirs()
                return dir.absolutePath
            }
        }
    }
    
    single<BookImporter> { DesktopBookImporter(get<LibraryPathProvider>().getLibraryPath()) }
}
