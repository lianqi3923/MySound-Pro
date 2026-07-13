package io.github.mysoundpro.core.log

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class RuntimeLoggerTest {
    @AfterEach
    fun reset() = RuntimeLogger.configure(false)

    @Test
    fun `runtime debug switch changes existing logger behavior`() {
        val output = mutableListOf<String>()
        RuntimeLogger.configure(false, output::add)
        val capturedReference: Logger = RuntimeLogger

        capturedReference.debug(LogEvent("hidden"))
        RuntimeLogger.configure(true, output::add)
        capturedReference.debug(LogEvent("visible", mapOf("url" to "https://example.test")))

        assertThat(output).singleElement().asString().contains("visible", "https://example.test")
    }
}
