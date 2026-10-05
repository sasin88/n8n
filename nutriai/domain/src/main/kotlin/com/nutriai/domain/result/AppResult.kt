package com.nutriai.domain.result

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

/**
 * Errores de negocio. La UI los traduce a mensajes humanos; nunca se muestra un error técnico.
 */
sealed interface AppError {
    data object NoInternet : AppError
    data object ServiceUnavailable : AppError
    data object InvalidAiResponse : AppError
    data object UnreadableImage : AppError
    data class UnknownFood(val name: String) : AppError
    data class PermissionDenied(val permission: Permission) : AppError
    data object Storage : AppError
    data object Unexpected : AppError

    enum class Permission { CAMERA }
}
