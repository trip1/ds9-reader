package com.example.ds9reader.di

import com.example.ds9reader.createSqlDriver
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { createSqlDriver() }
}
