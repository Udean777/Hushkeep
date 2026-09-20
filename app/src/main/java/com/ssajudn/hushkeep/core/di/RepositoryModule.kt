package com.ssajudn.hushkeep.core.di

import android.content.Context
import androidx.work.WorkManager
import com.ssajudn.hushkeep.data.local.HushkeepDatabase
import com.ssajudn.hushkeep.data.local.LocalPreviewAuthRepository
import com.ssajudn.hushkeep.data.local.SessionStore
import com.ssajudn.hushkeep.data.remote.SupabaseAuthRepository
import com.ssajudn.hushkeep.data.repository.LocalMemoryRepository
import com.ssajudn.hushkeep.domain.repository.AuthRepository
import com.ssajudn.hushkeep.domain.repository.MemoryRepository
import io.github.jan.supabase.SupabaseClient
import java.io.File

object RepositoryModule {
    fun provideAuthRepository(
        client: SupabaseClient?,
        sessionStore: SessionStore,
    ): AuthRepository = client?.let(::SupabaseAuthRepository)
        ?: LocalPreviewAuthRepository(sessionStore)

    fun provideMemoryRepository(
        context: Context,
        database: HushkeepDatabase,
        workManager: WorkManager,
        client: SupabaseClient?,
    ): MemoryRepository = LocalMemoryRepository(
        database = database,
        contentResolver = context.applicationContext.contentResolver,
        mediaDirectory = File(context.applicationContext.filesDir, "hushkeep-media"),
        workManager = workManager,
        supabaseClient = client,
    )
}
