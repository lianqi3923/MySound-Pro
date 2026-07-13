package io.github.mysoundpro.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CoreInfrastructurePresenceTest {
    @Test
    fun `configuration logging and search infrastructure are available`() {
        val expectedMethods = mapOf(
            "io.github.mysoundpro.core.config.SourceConfigService" to setOf("load"),
            "io.github.mysoundpro.core.log.DebugLogger" to setOf("debug"),
            "io.github.mysoundpro.core.log.NoOpLogger" to setOf("debug"),
            "io.github.mysoundpro.core.search.SearchOrchestrator" to setOf("search"),
        )

        expectedMethods.forEach { (className, methods) ->
            val actual = Class.forName(className).methods.mapTo(mutableSetOf()) { it.name }
            assertThat(actual).containsAll(methods)
        }
    }
}
