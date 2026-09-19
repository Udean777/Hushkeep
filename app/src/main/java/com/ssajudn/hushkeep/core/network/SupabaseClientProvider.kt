package com.ssajudn.hushkeep.core.network

import com.ssajudn.hushkeep.core.config.AppConfig
import com.ssajudn.hushkeep.core.config.DeepLinks
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    fun create(): SupabaseClient? {
        if (!AppConfig.isSupabaseConfigured) return null

        return createSupabaseClient(
            supabaseUrl = AppConfig.supabaseUrl,
            supabaseKey = AppConfig.supabaseAnonKey,
        ) {
            install(Auth) {
                scheme = DeepLinks.SCHEME
                host = DeepLinks.AUTH_HOST
            }
            install(Postgrest)
            install(Storage)
        }
    }
}
