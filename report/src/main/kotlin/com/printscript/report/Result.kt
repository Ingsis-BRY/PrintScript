package com.printscript.report

sealed interface Result<out T>

data class Success<T>(
    val value: T,
) : Result<T>

data class Failure(
    val error: Diagnostic,
) : Result<Nothing>

/**
* use if [transform] can't fail, else use flatMap
*/
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> =
    when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

/**
* use if [transform] can fail
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
    when (this) {
        is Success -> transform(value)
        is Failure -> this
    }

/**
* returns unwrapped value R
*/
inline fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Diagnostic) -> R,
): R =
    when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(error)
    }
