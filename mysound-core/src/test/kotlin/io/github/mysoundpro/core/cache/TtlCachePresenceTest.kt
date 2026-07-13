package io.github.mysoundpro.core.cache

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TtlCachePresenceTest {
    @Test
    fun `bounded ttl cache is available as shared infrastructure`() {
        val type = Class.forName("io.github.mysoundpro.core.cache.TtlCache")

        assertThat(type.methods.map { it.name })
            .contains("get", "put", "invalidate", "invalidateAll", "getOrPut")
    }
}
