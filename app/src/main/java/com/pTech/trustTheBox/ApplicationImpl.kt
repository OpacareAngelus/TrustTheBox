package com.pTech.trustTheBox

import android.app.Application
import com.pTech.trustTheBox.sdk.initStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class ApplicationImpl : Application() {
    val appModule = module {
        single { this@ApplicationImpl }
    }

    override fun onCreate() {
        super.onCreate()
        initStorage(this@ApplicationImpl)
        startKoin {
            androidContext(this@ApplicationImpl)
            modules(appModule)
        }
    }
}