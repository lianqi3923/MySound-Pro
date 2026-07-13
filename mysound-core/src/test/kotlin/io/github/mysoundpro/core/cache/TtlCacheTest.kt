package io.github.mysoundpro.core.cache

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TtlCacheTest {
    @Test
    fun `entry is available before ttl and removed at expiry`() {
        var nowMs = 1_000L
        val cache = TtlCache<String, String>(maxEntries = 8, nowMs = { nowMs })

        cache.put("book", "value", ttlMs = 300_000L)

        assertThat(cache.get("book")).isEqualTo("value")
        nowMs += 299_999L
        assertThat(cache.get("book")).isEqualTo("value")
        nowMs += 1L
        assertThat(cache.get("book")).isNull()
    }

    @Test
    fun `get or put invokes loader once while value is fresh`() {
        var loads = 0
        val cache = TtlCache<String, String>(maxEntries = 8, nowMs = { 0L })

        val first = cache.getOrPut("keyword", ttlMs = 300_000L) { "value-${++loads}" }
        val second = cache.getOrPut("keyword", ttlMs = 300_000L) { "value-${++loads}" }

        assertThat(first).isEqualTo("value-1")
        assertThat(second).isEqualTo("value-1")
        assertThat(loads).isEqualTo(1)
    }

    @Test
    fun `bounded cache evicts least recently used entry`() {
        val cache = TtlCache<String, String>(maxEntries = 2, nowMs = { 0L })
        cache.put("first", "1", ttlMs = 300_000L)
        cache.put("second", "2", ttlMs = 300_000L)
        cache.get("first")

        cache.put("third", "3", ttlMs = 300_000L)

        assertThat(cache.get("first")).isEqualTo("1")
        assertThat(cache.get("second")).isNull()
        assertThat(cache.get("third")).isEqualTo("3")
    }

    @Test
    fun `explicit invalidation removes selected or all entries`() {
        val cache = TtlCache<String, String>(maxEntries = 8, nowMs = { 0L })
        cache.put("a", "1", ttlMs = 300_000L)
        cache.put("b", "2", ttlMs = 300_000L)

        cache.invalidate("a")
        assertThat(cache.get("a")).isNull()
        assertThat(cache.get("b")).isEqualTo("2")

        cache.invalidateAll()
        assertThat(cache.get("b")).isNull()
    }
}
