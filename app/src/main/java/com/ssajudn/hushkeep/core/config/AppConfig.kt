package com.ssajudn.hushkeep.core.config

import com.ssajudn.hushkeep.BuildConfig

object AppConfig {
    val applicationId: String = BuildConfig.APPLICATION_ID
    val versionName: String = BuildConfig.VERSION_NAME
    val environment: String
        get() = BuildConfigFields.environment

    val supabaseUrl: String
        get() = BuildConfigFields.supabaseUrl

    val supabaseAnonKey: String
        get() = BuildConfigFields.supabaseAnonKey

    val isSupabaseConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()
}
