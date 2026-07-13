package io.github.mysoundpro.core.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

class RetryPolicyTest {
    private val policy = RetryPolicy(maxRetries = 2, initialDelayMs = 10L)

    @Test
    fun `retries recoverable get failures within the configured limit`() {
        assertThat(policy.shouldRetry(500, null, retryCount = 0)).isTrue()
        assertThat(policy.shouldRetry(429, null, retryCount = 1)).isTrue()
        assertThat(policy.shouldRetry(null, IOException("reset"), retryCount = 1)).isTrue()
        assertThat(policy.shouldRetry(500, null, retryCount = 2)).isFalse()
    }

    @Test
    fun `does not retry permanent client errors`() {
        assertThat(policy.shouldRetry(404, null, retryCount = 0)).isFalse()
        assertThat(policy.shouldRetry(401, null, retryCount = 0)).isFalse()
    }
}
