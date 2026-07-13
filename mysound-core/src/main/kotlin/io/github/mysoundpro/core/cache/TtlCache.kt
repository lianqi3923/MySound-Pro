package io.github.mysoundpro.core.cache

/** 有界、可注入时钟的进程内 TTL 缓存。 */
class TtlCache<K : Any, V : Any>(
    private val maxEntries: Int,
    private val nowMs: () -> Long,
) {
    private data class Entry<V>(val value: V, val expiresAtMs: Long)

    private val entries = LinkedHashMap<K, Entry<V>>(16, 0.75f, true)

    init {
        require(maxEntries > 0) { "maxEntries must be greater than zero" }
    }

    @Synchronized
    fun get(key: K): V? {
        val entry = entries[key] ?: return null
        if (nowMs() >= entry.expiresAtMs) {
            entries.remove(key)
            return null
        }
        return entry.value
    }

    @Synchronized
    fun put(key: K, value: V, ttlMs: Long) {
        require(ttlMs > 0) { "ttlMs must be greater than zero" }
        removeExpired()
        entries[key] = Entry(value, nowMs() + ttlMs)
        while (entries.size > maxEntries) {
            val eldest = entries.entries.iterator()
            if (eldest.hasNext()) {
                eldest.next()
                eldest.remove()
            }
        }
    }

    @Synchronized
    fun invalidate(key: K) {
        entries.remove(key)
    }

    @Synchronized
    fun invalidateAll() {
        entries.clear()
    }

    @Synchronized
    fun getOrPut(key: K, ttlMs: Long, loader: () -> V): V {
        get(key)?.let { return it }
        return loader().also { put(key, it, ttlMs) }
    }

    private fun removeExpired() {
        val now = nowMs()
        entries.entries.removeAll { it.value.expiresAtMs <= now }
    }
}
