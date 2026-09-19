package com.ssajudn.hushkeep.core.di

import com.ssajudn.hushkeep.core.common.Clock

object WorkerModule {
    fun provideClock(): Clock = Clock.System
}
