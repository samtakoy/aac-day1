package com.example.day.aigateway.middleware

import kotlinx.coroutines.sync.Semaphore

class ConcurrencyLimiter(limit: Int) {
    private val semaphore = Semaphore(limit)

    suspend fun <T> withLimit(block: suspend () -> T): T? {
        if (!semaphore.tryAcquire()) return null
        return try {
            block()
        } finally {
            semaphore.release()
        }
    }
}
