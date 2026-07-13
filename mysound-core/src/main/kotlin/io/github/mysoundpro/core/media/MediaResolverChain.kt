package io.github.mysoundpro.core.media

import io.github.mysoundpro.api.MediaFormat
import io.github.mysoundpro.api.PlayInfo
import io.github.mysoundpro.core.http.HttpClient
import io.github.mysoundpro.core.http.HttpRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jsoup.Jsoup
import java.net.URI

/**
 * 解析公开播放页的通用责任链。
 *
 * 顺序固定为直接地址、HTML 媒体标签、JSON、静态脚本、iframe。站点 Parser
 * 只负责提供播放页地址，避免在每个站点重复实现播放器探测逻辑。
 */
class MediaResolverChain(
    private val httpClient: HttpClient,
    private val maxIframeDepth: Int = 2,
) {
    suspend fun resolve(url: String): PlayInfo = resolve(url, depth = 0, rootReferer = null)

    private suspend fun resolve(url: String, depth: Int, rootReferer: String?): PlayInfo {
        mediaFormat(url)?.let { return PlayInfo(url, it, publicHeaders(rootReferer)) }
        check(depth <= maxIframeDepth) { "iframe depth exceeded at $url" }

        val response = httpClient.execute(HttpRequest(url, referer = rootReferer))
        check(response.code in 200..299) { "media page HTTP ${response.code}: $url" }
        val pageUrl = response.finalUrl
        val document = Jsoup.parse(response.text(), pageUrl)

        val domCandidate = document.select("audio[src], audio source[src], source[src]")
            .asSequence()
            .map { it.absUrl("src") }
            .firstOrNull(::isMediaUrl)
        if (domCandidate != null) return playInfo(domCandidate, pageUrl)

        val jsonCandidate = document.select("script[type=application/json], script[type=application/ld+json]")
            .asSequence()
            .flatMap { jsonMediaCandidates(it.data()).asSequence() }
            .map { resolveUrl(pageUrl, it) }
            .firstOrNull(::isMediaUrl)
        if (jsonCandidate != null) return playInfo(jsonCandidate, pageUrl)

        val scriptCandidate = document.select("script:not([src])")
            .asSequence()
            .flatMap { scriptMediaCandidates(it.data()).asSequence() }
            .map { resolveUrl(pageUrl, it) }
            .firstOrNull(::isMediaUrl)
        if (scriptCandidate != null) return playInfo(scriptCandidate, pageUrl)

        val iframe = document.select("iframe[src]").firstOrNull()?.absUrl("src")
        if (!iframe.isNullOrBlank()) return resolve(iframe, depth + 1, rootReferer = pageUrl)

        error("no public media candidate found at $pageUrl")
    }

    private fun playInfo(url: String, referer: String) = PlayInfo(
        url = url,
        format = mediaFormat(url) ?: MediaFormat.UNKNOWN,
        headers = publicHeaders(referer),
    )

    private fun publicHeaders(referer: String?) = buildMap {
        referer?.let { put("Referer", it) }
        put("User-Agent", HttpRequest.DEFAULT_USER_AGENT)
    }

    private fun jsonMediaCandidates(raw: String): List<String> = runCatching {
        val root = Json.parseToJsonElement(raw)
        buildList { collectJsonStrings(root, this) }.filter(::isMediaUrl)
    }.getOrDefault(emptyList())

    private fun collectJsonStrings(element: JsonElement, output: MutableList<String>) {
        when (element) {
            is JsonObject -> element.values.forEach { collectJsonStrings(it, output) }
            is JsonArray -> element.forEach { collectJsonStrings(it, output) }
            is JsonPrimitive -> if (element.isString) output += element.content
        }
    }

    private fun scriptMediaCandidates(script: String): List<String> = MEDIA_LITERAL
        .findAll(script.replace("\\/", "/"))
        .map { it.groupValues[1] }
        .toList()

    private fun resolveUrl(baseUrl: String, candidate: String): String =
        runCatching { URI(baseUrl).resolve(candidate).toString() }.getOrDefault(candidate)

    private fun isMediaUrl(url: String): Boolean = mediaFormat(url) != null

    private fun mediaFormat(url: String): MediaFormat? {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.endsWith(".mp3") -> MediaFormat.MP3
            path.endsWith(".m3u8") -> MediaFormat.M3U8
            path.endsWith(".aac") || path.endsWith(".m4a") -> MediaFormat.AAC
            else -> null
        }
    }

    private companion object {
        val MEDIA_LITERAL = Regex("[\\\"']([^\\\"']+\\.(?:mp3|m3u8|aac|m4a)(?:\\?[^\\\"']*)?)[\\\"']", RegexOption.IGNORE_CASE)
    }
}
