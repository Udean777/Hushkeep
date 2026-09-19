package com.ssajudn.hushkeep

import android.content.Context
import com.ssajudn.hushkeep.core.di.AppModule
import com.ssajudn.hushkeep.core.di.DatabaseModule
import com.ssajudn.hushkeep.core.di.NetworkModule
import com.ssajudn.hushkeep.core.di.RepositoryModule
import com.ssajudn.hushkeep.data.local.HushkeepDatabase
import com.ssajudn.hushkeep.data.local.SessionStore
import com.ssajudn.hushkeep.domain.repository.AuthRepository
import com.ssajudn.hushkeep.domain.repository.MemoryRepository

/**
 * Manual composition root. It keeps dependencies explicit while the Hilt
 * Gradle plugin remains incompatible with this project's AGP version.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: HushkeepDatabase = DatabaseModule.provideDatabase(appContext)
    val sessionStore: SessionStore = SessionStore(appContext)
    val workManager = DatabaseModule.provideWorkManager(appContext)
    val exportManager = AppModule.provideExportManager(appContext, database)
    val memoryRepository: MemoryRepository = RepositoryModule.provideMemoryRepository(
        context = appContext,
        database = database,
        workManager = workManager,
    )

    val authRepository: AuthRepository = RepositoryModule.provideAuthRepository(
        client = NetworkModule.provideSupabaseClient(),
        sessionStore = sessionStore,
    )
}
