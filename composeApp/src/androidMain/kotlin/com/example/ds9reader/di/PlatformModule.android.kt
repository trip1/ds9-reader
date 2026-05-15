package com.example.ds9reader.di

import android.content.Context
import com.example.ds9reader.AndroidDriverFactory
import com.example.ds9reader.data.AndroidBookImporter
import com.example.ds9reader.data.BookImporter
import com.example.ds9reader.domain.LibraryPathProvider
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual fun platformModule(): Module = module {
    single { AndroidDriverFactory(get()).create() }
    
    single<LibraryPathProvider> {
        object : LibraryPathProvider {
            override fun getLibraryPath(): String {
                val context = get<Context>()
                val dir = File(context.filesDir, "library")
                if (!dir.exists()) dir.mkdirs()
                return dir.absolutePath
            }
        }
    }
    
    single<BookImporter> { AndroidBookImporter(get()) }
}
