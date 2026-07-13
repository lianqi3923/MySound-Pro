package io.github.mysoundpro.core.source

import io.github.mysoundpro.api.AudioSource
import io.github.mysoundpro.api.Book
import io.github.mysoundpro.api.Chapter
import io.github.mysoundpro.core.cache.TtlCache

/** 为所有来源统一提供 5 分钟的搜索、详情和章节缓存。 */
abstract class CachedAudioSource(
    nowMs: () -> Long = System::currentTimeMillis,
) : AudioSource {
    private val searchCache = TtlCache<String, List<Book>>(64, nowMs)
    private val detailCache = TtlCache<String, Book>(128, nowMs)
    private val chapterCache = TtlCache<String, List<Chapter>>(128, nowMs)

    final override suspend fun search(keyword: String): List<Book> {
        val key = keyword.trim()
        searchCache.get(key)?.let { return it }
        return loadSearch(key).also { searchCache.put(key, it, TTL_MS) }
    }

    final override suspend fun detail(url: String): Book {
        detailCache.get(url)?.let { return it }
        return loadDetail(url).also { detailCache.put(url, it, TTL_MS) }
    }

    final override suspend fun chapters(url: String): List<Chapter> {
        chapterCache.get(url)?.let { return it }
        return loadChapters(url).also { chapterCache.put(url, it, TTL_MS) }
    }

    protected abstract suspend fun loadSearch(keyword: String): List<Book>
    protected abstract suspend fun loadDetail(url: String): Book
    protected abstract suspend fun loadChapters(url: String): List<Chapter>

    private companion object {
        const val TTL_MS = 5 * 60 * 1_000L
    }
}
