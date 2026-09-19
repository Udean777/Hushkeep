package com.ssajudn.hushkeep.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ssajudn.hushkeep.AppContainer

class HushkeepViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HushkeepViewModel::class.java)) {
            return HushkeepViewModel(
                authRepository = container.authRepository,
                memoryRepository = container.memoryRepository,
                exportManager = container.exportManager,
            ) as T
        }
        error("Unknown ViewModel: ${modelClass.name}")
    }
}
