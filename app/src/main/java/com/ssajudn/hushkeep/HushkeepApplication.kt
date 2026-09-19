package com.ssajudn.hushkeep

import android.app.Application

class HushkeepApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
