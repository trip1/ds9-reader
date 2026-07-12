package com.example.ds9reader

import android.app.Application
import com.example.ds9reader.di.appModule
import com.example.ds9reader.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class DS9ReaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger()
                androidContext(this@DS9ReaderApp)
                modules(appModule, platformModule())
            }
        }
    }
}
