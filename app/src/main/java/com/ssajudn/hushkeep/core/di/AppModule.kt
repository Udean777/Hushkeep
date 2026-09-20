package com.ssajudn.hushkeep.core.di

import android.content.Context
import com.ssajudn.hushkeep.core.media.ExportManager
import com.ssajudn.hushkeep.data.local.HushkeepDatabase
import io.github.jan.supabase.SupabaseClient

object AppModule {
    fun provideExportManager(
        context: Context,
        database: HushkeepDatabase,
        supabaseClient: SupabaseClient?,
    ): ExportManager = ExportManager(
        mediaObjectDao = database.mediaObjectDao(),
        memoryDao = database.memoryDao(),
        albumDao = database.albumDao(),
        contentResolver = context.contentResolver,
        supabaseClient = supabaseClient,
    )
}
