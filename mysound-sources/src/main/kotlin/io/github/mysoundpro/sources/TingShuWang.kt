package io.github.mysoundpro.sources

import io.github.mysoundpro.api.AudioSource
import io.github.mysoundpro.api.AudioSourceMetadata
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

/** 听书网公开 HTML Parser，不依赖登录、私有 Cookie、Token 或签名。 */
@AudioSourceMetadata(id = "tingshuwang", name = "听书网", host = "https://www.tingshuwang.cc")
object TingShuWang : AudioSource by TingShuWangSource()

internal class TingShuWangSource(
    private val httpClient: HttpClient = HttpClientFactory.create(),
) : CachedAudioSource() {
    override val sourceId = "tingshuwang"
    override val name = "听书网"
    override val host = "https://www.tingshuwang.cc"
    private val mediaResolver = MediaResolverChain(httpClient)

    override suspend fun loadSearch(keyword: String): List<Book> {
        if (keyword.isBlank()) return emptyList()
        val url = "$host/so/${keyword.urlEncoded()}/"
        return fetchDocument(url, SourceOperation.SEARCH).select(".book-list-wrapper .book").mapNotNull { card ->
            val link = card.selectFirst(".book-info h5 a[href]") ?: return@mapNotNull null
            val title = link.text().trim()
            if (title.isBlank()) return@mapNotNull null
            Book(
                title = title,
                author = card.selectFirst(".author a")?.text()?.trim()?.ifBlank { null },
                narrator = card.selectFirst(".anchor a")?.text()?.trim()?.ifBlank { null },
                coverUrl = card.selectFirst(".book-img img")?.absUrl("src")?.ifBlank { null },
                category = card.selectFirst(".category a")?.text()?.trim()?.ifBlank { null },
                description = card.selectFirst(".intro")?.text()?.trim()?.ifBlank { null },
                sourceId = sourceId,
                sourceName = name,
                detailUrl = link.absUrl("href"),
            )
        }.distinctBy { it.detailUrl }
    }

    override suspend fun loadDetail(url: String): Book {
        val document = fetchDocument(url, SourceOperation.DETAIL)
        val title = document.selectFirst(".book-title h1")?.text()?.trim().orEmpty()
        if (title.isBlank()) throw parseError(SourceOperation.DETAIL, url, "detail:.book-title h1")
        val description = document.selectFirst(".book-desc-text")?.clone()?.also {
            it.select(".other, .book-tips").remove()
        }?.text()?.trim()?.ifBlank { null }
        return Book(
            title = title,
            author = document.selectFirst(".book-info .author a")?.text()?.trim()?.ifBlank { null },
            narrator = document.selectFirst(".book-info .auchor a, .book-info .anchor a")?.text()?.trim()?.ifBlank { null },
            coverUrl = document.selectFirst(".book-cover img")?.absUrl("src")?.ifBlank { null },
            category = document.selectFirst(".book-info .category a")?.text()?.trim()?.ifBlank { null },
            description = description,
            sourceId = sourceId,
            sourceName = name,
            detailUrl = url,
        )
    }

    override suspend fun loadChapters(url: String): List<Chapter> {
        // 站点页面按新到旧展示；插件统一返回旧到新并重新生成稳定序号。
        val links = fetchDocument(url, SourceOperation.CHAPTERS)
            .select(".book-chpater-list a[href], .book-chapter-list a[href]")
            .asReversed()
        if (links.isEmpty()) throw parseError(SourceOperation.CHAPTERS, url, "chapters:.book-chpater-list")
        return links.mapIndexed { index, link ->
            Chapter(link.text().trim().ifBlank { "第 ${index + 1} 集" }, link.absUrl("href"), index)
        }
    }

    override suspend fun play(url: String): PlayInfo = try {
        mediaResolver.resolve(url)
    } catch (failure: Exception) {
        throw SourceException(sourceId, SourceOperation.PLAY, url, SourceErrorKind.MEDIA_NOT_FOUND, "play:resolver", "未找到公开媒体地址", failure)
    }

    private suspend fun fetchDocument(url: String, operation: SourceOperation): Document {
        val response = httpClient.execute(HttpRequest(url))
        if (response.code !in 200..299) throw httpError(operation, url, response.code)
        return Jsoup.parse(response.text(), response.finalUrl)
    }

    private fun httpError(operation: SourceOperation, url: String, code: Int) =
        SourceException(sourceId, operation, url, SourceErrorKind.HTTP_STATUS, "http", "HTTP $code")

    private fun parseError(operation: SourceOperation, url: String, location: String) =
        SourceException(sourceId, operation, url, SourceErrorKind.PARSE, location, "听书网页面结构不完整")

    private fun String.urlEncoded() = URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}
