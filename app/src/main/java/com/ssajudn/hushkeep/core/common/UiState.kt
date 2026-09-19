package com.ssajudn.hushkeep.core.common

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Content<T>(val value: T) : UiState<T>
    data class Error(val error: AppError) : UiState<Nothing>
}
