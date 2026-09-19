package com.ssajudn.hushkeep.core.di

import com.ssajudn.hushkeep.core.network.SupabaseClientProvider
import io.github.jan.supabase.SupabaseClient

object NetworkModule {
    fun provideSupabaseClient(): SupabaseClient? = SupabaseClientProvider.create()
}
