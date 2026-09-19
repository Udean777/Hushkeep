package com.ssajudn.hushkeep.core.common

sealed interface AppError {
    data object Unknown : AppError
    data object NetworkUnavailable : AppError
    data object NotFound : AppError
    data object AuthenticationRequired : AppError
    data class Validation(val message: String) : AppError
    data class Storage(val message: String, val cause: Throwable? = null) : AppError
    data class Remote(val message: String, val cause: Throwable? = null) : AppError
}
