package io.github.mysoundpro.host

import com.github.eprendre.tingshu.sources.AudioUrlCustomExtractor
import io.github.mysoundpro.api.AudioSource
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * 连接 MyTingShu 全局音频提取器和多个独立 [AudioSource]。
 *
 * 章节详情加载后会记录精确路由；主机名匹配只作为兼容旧收藏的回退方式，
 * 并使用 URI.host 比较，避免字符串前缀带来的相似域名误判。
 */
internal object MyTingShuRuntime {
    private val chapterRoutes = ConcurrentHashMap<String, AudioSource>()
    private val audioHeaders = ConcurrentHashMap<String, Map<String, String>>()

    @Volatile
    private var sources: List<AudioSource> = emptyList()

    fun install(registeredSources: List<AudioSource>) {
        sources = registeredSources.toList()
        AudioUrlCustomExtractor.setUp(::resolveAudio)
    }

    fun track(source: AudioSource, chapterUrls: Iterable<String>) {
        chapterUrls.forEach { chapterRoutes[it] = source }
    }

    fun headers(audioUrl: String): Map<String, String> = audioHeaders[audioUrl].orEmpty()

    private fun resolveAudio(chapterUrl: String): String {
        val source = chapterRoutes[chapterUrl] ?: sourceForHost(chapterUrl)
            ?: error("No AudioSource can handle chapter URL: $chapterUrl")
        val playInfo = runBlocking { source.play(chapterUrl) }
        if (playInfo.headers.isNotEmpty()) audioHeaders[playInfo.url] = playInfo.headers
        return playInfo.url
    }

    private fun sourceForHost(url: String): AudioSource? {
        val chapterHost = runCatching { URI(url).host }.getOrNull() ?: return null
        return sources.firstOrNull { source ->
            runCatching { URI(source.host).host.equals(chapterHost, ignoreCase = true) }.getOrDefault(false)
        }
    }
}
