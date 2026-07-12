package com.example.ds9reader

import android.app.Application
import android.util.Log
import com.example.ds9reader.di.appModule
import com.example.ds9reader.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DS9ReaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            if (GlobalContext.getOrNull() == null) {
                startKoin {
                    androidLogger(Level.ERROR)
                    androidContext(this@DS9ReaderApp)
                    modules(appModule, platformModule())
                }
            }
        } catch (t: Throwable) {
            Log.e("DS9ReaderApp", "Koin init failed", t)
        }
    }

    companion object {
        lateinit var instance: DS9ReaderApp
            private set
    }
}
