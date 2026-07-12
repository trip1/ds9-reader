package com.example.ds9reader

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ds9reader.di.appModule
import com.example.ds9reader.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            ensureKoin()
        } catch (t: Throwable) {
            Log.e("MainActivity", "Koin init failed", t)
        }
        setContent {
            App()
        }
    }

    private fun ensureKoin() {
        if (GlobalContext.getOrNull() != null) return
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(applicationContext)
            modules(appModule, platformModule())
        }
    }
}
