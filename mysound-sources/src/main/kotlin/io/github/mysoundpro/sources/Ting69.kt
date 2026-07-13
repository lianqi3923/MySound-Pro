package io.github.mysoundpro.sources

import io.github.mysoundpro.api.AudioSource
import io.github.mysoundpro.api.Book
import io.github.mysoundpro.api.Chapter
import io.github.mysoundpro.api.PlayInfo
import io.github.mysoundpro.api.SourceErrorKind
import io.github.mysoundpro.api.SourceException
import io.github.mysoundpro.api.SourceOperation
import io.github.mysoundpro.core.http.HttpClient
import io.github.mysoundpro.core.http.HttpClientFactory
import io.github.mysoundpro.core.http.HttpRequest
import io.github.mysoundpro.core.media.MediaResolverChain
import io.github.mysoundpro.core.source.CachedAudioSource
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder

/** 69 听书公开 HTML Parser；没有登录、令牌或签名逻辑。 */
/** 搜索页当前要求验证码，因此仅保留解析研究代码，不注册到生产来源。 */
object Ting69 : AudioSource by Ting69Source()

internal class Ting69Source(
    private val httpClient: HttpClient = HttpClientFactory.create(),
) : CachedAudioSource() {
    override val sourceId = "ting69"
    override val name = "69听书"
    override val host = "https://www.ting69.com"
    private val mediaResolver = MediaResolverChain(httpClient)

    override suspend fun loadSearch(keyword: String): List<Book> {
        if (keyword.isBlank()) return emptyList()
        val encoded = URLEncoder.encode(keyword, Charsets.UTF_8.name()).replace("+", "%20")
        val url = "$host/search.php?searchword=$encoded"
        val document = fetchDocument(url, SourceOperation.SEARCH)
        return document.select(".style-img").mapNotNull { card ->
            val link = card.selectFirst("a[href*=/show/]") ?: return@mapNotNull null
            val title = card.selectFirst("h2 a[href*=/show/]")?.text()?.trim().orEmpty()
            if (title.isBlank()) return@mapNotNull null
            val summary = card.selectFirst("p.f-gray")?.text().orEmpty()
            Book(
                title = title,
                author = between(summary, "作者：", "，由"),
                narrator = between(summary, "，由", "播音"),
                coverUrl = card.selectFirst("img")?.absUrl("src")?.ifBlank { null },
                category = card.selectFirst("a[href*=/list/]")?.text()?.trim(),
                sourceId = sourceId,
                sourceName = name,
                detailUrl = link.absUrl("href"),
            )
        }.distinctBy { it.detailUrl }
    }

    override suspend fun loadDetail(url: String): Book {
        val document = fetchDocument(url, SourceOperation.DETAIL)
        val title = document.selectFirst(".style-img section h1")?.text()?.trim().orEmpty()
        if (title.isBlank()) throw parseError(SourceOperation.DETAIL, url, "detail:h1")
        val credits = document.selectFirst(".style-img section p:contains(作者：)")
        val people = credits?.select("a[href*=searchword]").orEmpty().map { it.text().trim() }
        val description = document.select(".style-img section p")
            .firstOrNull { it.text().trim().startsWith("内容介绍：") }
            ?.text()?.substringAfter("内容介绍：")?.trim()
        return Book(
            title = title,
            author = people.getOrNull(0),
            narrator = people.getOrNull(1),
            coverUrl = document.selectFirst(".img-100 img")?.absUrl("src")?.ifBlank { null },
            category = document.select(".place a[href*=/list/]").lastOrNull()?.text()?.trim(),
            description = description,
            sourceId = sourceId,
            sourceName = name,
            detailUrl = url,
        )
    }

    override suspend fun loadChapters(url: String): List<Chapter> {
        val document = fetchDocument(url, SourceOperation.CHAPTERS)
        val chapters = document.select("#yuedu a[href*=/play/]").mapIndexed { index, link ->
            Chapter(
                title = link.attr("title").ifBlank { link.text() }.trim(),
                url = link.absUrl("href"),
                index = index,
            )
        }
        if (chapters.isEmpty()) throw parseError(SourceOperation.CHAPTERS, url, "chapters:#yuedu")
        return chapters
    }

    override suspend fun play(url: String): PlayInfo = try {
        mediaResolver.resolve(url)
    } catch (failure: Exception) {
        throw SourceException(sourceId, SourceOperation.PLAY, url, SourceErrorKind.MEDIA_NOT_FOUND, "play:resolver", "未找到公开媒体地址", failure)
    }

    private suspend fun fetchDocument(url: String, operation: SourceOperation): Document {
        val response = httpClient.execute(HttpRequest(url))
        if (response.code !in 200..299) {
            throw SourceException(sourceId, operation, url, SourceErrorKind.HTTP_STATUS, "http", "HTTP ${response.code}")
        }
        return Jsoup.parse(response.text("GB18030"), response.finalUrl)
    }

    private fun parseError(operation: SourceOperation, url: String, location: String) =
        SourceException(sourceId, operation, url, SourceErrorKind.PARSE, location, "69听书页面结构不完整")

    private fun between(text: String, start: String, end: String): String? = text
        .takeIf { start in it && end in it }
        ?.substringAfter(start)?.substringBefore(end)?.trim()?.ifBlank { null }
}
