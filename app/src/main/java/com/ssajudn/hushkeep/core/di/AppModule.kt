package com.ssajudn.hushkeep.core.di

import android.content.Context
import com.ssajudn.hushkeep.core.media.ExportManager
import com.ssajudn.hushkeep.data.local.HushkeepDatabase

object AppModule {
    fun provideExportManager(
        context: Context,
        database: HushkeepDatabase,
    ): ExportManager = ExportManager(
        mediaObjectDao = database.mediaObjectDao(),
        contentResolver = context.contentResolver,
    )
}
