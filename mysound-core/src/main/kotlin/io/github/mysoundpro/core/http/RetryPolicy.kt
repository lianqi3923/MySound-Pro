package io.github.mysoundpro.core.http

import java.io.IOException

data class RetryPolicy(
    val maxRetries: Int = 2,
    val initialDelayMs: Long = 100L,
) {
    fun shouldRetry(statusCode: Int?, failure: IOException?, retryCount: Int): Boolean {
        if (retryCount >= maxRetries) return false
        if (failure != null) return true
        return statusCode == 408 || statusCode == 429 || statusCode in 500..599
    }

    fun delayMs(retryCount: Int): Long = initialDelayMs * (1L shl retryCount.coerceAtMost(10))
}
