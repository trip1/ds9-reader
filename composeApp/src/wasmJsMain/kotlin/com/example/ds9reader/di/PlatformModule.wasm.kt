package com.example.ds9reader.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.example.ds9reader.domain.BookStorage
import com.example.ds9reader.domain.FilePicker
import com.example.ds9reader.domain.PickerResult
import com.example.ds9reader.platform.WasmBookStorage
import org.koin.core.module.Module
import org.koin.dsl.module
import org.w3c.dom.Worker

actual fun platformModule(): Module = module {
    single<SqlDriver> {
        // sql.js worker driver; requires webpack copy of worker assets for full production.
        WebWorkerDriver(Worker(js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)""")))
    }
    single<BookStorage> { WasmBookStorage() }
    single<FilePicker> {
        object : FilePicker {
            override suspend fun pickEpubFile(): PickerResult =
                PickerResult.Error("Use Calibre sync on web builds")
            override suspend fun pickEpubDirectory(): PickerResult =
                PickerResult.Error("Use Calibre sync on web builds")
        }
    }
}
