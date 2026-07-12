package com.example.ds9reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ds9reader.di.appModule
import com.example.ds9reader.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureKoin()
        setContent {
            App()
        }
    }

    private fun ensureKoin() {
        if (GlobalContext.getOrNull() != null) return
        startKoin {
            androidLogger()
            androidContext(applicationContext)
            modules(appModule, platformModule())
        }
    }
}
