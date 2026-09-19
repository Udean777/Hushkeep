package com.ssajudn.hushkeep.core.di

import android.content.Context
import androidx.work.WorkManager
import com.ssajudn.hushkeep.data.local.HushkeepDatabase

object DatabaseModule {
    fun provideDatabase(context: Context): HushkeepDatabase =
        HushkeepDatabase.getInstance(context.applicationContext)

    fun provideWorkManager(context: Context): WorkManager =
        WorkManager.getInstance(context.applicationContext)
}
