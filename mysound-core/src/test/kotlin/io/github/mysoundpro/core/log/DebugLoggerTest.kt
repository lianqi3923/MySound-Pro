package io.github.mysoundpro.core.log

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DebugLoggerTest {
    @Test
    fun `release logger emits nothing`() {
        val output = mutableListOf<String>()
        val logger = DebugLogger(enabled = false, sink = output::add)

        logger.debug(LogEvent("request", mapOf("url" to "https://example.test")))

        assertThat(output).isEmpty()
    }

    @Test
    fun `debug logger emits fields and redacts credentials`() {
        val output = mutableListOf<String>()
        val logger = DebugLogger(enabled = true, sink = output::add)

        logger.debug(
            LogEvent(
                "request_failed",
                mapOf(
                    "url" to "https://example.test/book",
                    "Authorization" to "Bearer secret",
                    "cookie" to "session=secret",
                ),
            ),
        )

        assertThat(output).hasSize(1)
        val line = output.single()
        assertThat(line).contains("request_failed", "https://example.test/book", "<redacted>")
        assertThat(line).doesNotContain("Bearer secret", "session=secret")
    }
}
