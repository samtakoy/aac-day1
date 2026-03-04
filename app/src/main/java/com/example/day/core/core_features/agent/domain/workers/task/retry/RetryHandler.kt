package com.example.day.core.core_features.agent.domain.workers.task.retry

import kotlinx.coroutines.delay
import kotlin.math.pow

/**
 * Handles retry logic with exponential backoff for transient failures.
 * Used primarily for JSON parsing errors that may be resolved on retry.
 */
class RetryHandler(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000,
    private val maxDelayMs: Long = 10000,
    private val backoffMultiplier: Double = 2.0
) {

    /**
     * Result of retry operation.
     */
    sealed class RetryResult<out T> {
        data class Success<T>(val value: T) : RetryResult<T>()
        data class Failure(val errors: List<Throwable>) : RetryResult<Nothing>()
    }

    /**
     * Execute operation with retry logic.
     *
     * @param operation Suspend function to execute
     * @return RetryResult with either success value or list of all errors
     */
    suspend fun <T> executeWithRetry(
        operation: suspend (attempt: Int) -> T
    ): RetryResult<T> {
        val errors = mutableListOf<Throwable>()

        for (attempt in 0 until maxRetries) {
            try {
                val result = operation(attempt)
                return RetryResult.Success(result)
            } catch (e: Exception) {
                errors.add(e)

                // Don't delay on last attempt
                if (attempt < maxRetries - 1) {
                    val delayMs = calculateDelay(attempt)
                    delay(delayMs)
                }
            }
        }

        return RetryResult.Failure(errors)
    }

    /**
     * Execute operation with retry, filtering which exceptions trigger retry.
     *
     * @param retryableExceptions Set of exception classes that should trigger retry
     * @param operation Suspend function to execute
     * @return RetryResult with either success value or list of errors
     */
    suspend fun <T> executeWithRetry(
        retryableExceptions: Set<Class<out Exception>>,
        operation: suspend (attempt: Int) -> T
    ): RetryResult<T> {
        val errors = mutableListOf<Throwable>()

        for (attempt in 0 until maxRetries) {
            try {
                val result = operation(attempt)
                return RetryResult.Success(result)
            } catch (e: Exception) {
                errors.add(e)

                // Check if this exception type should trigger retry
                val shouldRetry = retryableExceptions.any { it.isInstance(e) }

                if (!shouldRetry) {
                    // Non-retryable exception, fail immediately
                    return RetryResult.Failure(errors)
                }

                // Don't delay on last attempt
                if (attempt < maxRetries - 1) {
                    val delayMs = calculateDelay(attempt)
                    delay(delayMs)
                }
            }
        }

        return RetryResult.Failure(errors)
    }

    /**
     * Calculate delay for given attempt using exponential backoff.
     *
     * @param attempt Current attempt number (0-based)
     * @return Delay in milliseconds
     */
    private fun calculateDelay(attempt: Int): Long {
        val delay = (initialDelayMs * backoffMultiplier.pow(attempt.toDouble())).toLong()
        return delay.coerceAtMost(maxDelayMs)
    }

    companion object {
        /**
         * Default retry handler for JSON parsing operations.
         */
        fun forJsonParsing(): RetryHandler = RetryHandler(
            maxRetries = 3,
            initialDelayMs = 500,
            maxDelayMs = 5000,
            backoffMultiplier = 2.0
        )

        /**
         * Default retry handler for network operations.
         */
        fun forNetwork(): RetryHandler = RetryHandler(
            maxRetries = 5,
            initialDelayMs = 1000,
            maxDelayMs = 30000,
            backoffMultiplier = 2.0
        )
    }
}