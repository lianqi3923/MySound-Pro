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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.net.URLDecoder
import java.net.URLEncoder

/** 云图有声公开微信网页 API Parser；公开 tenant ID 仅用于选择站点目录。 */
@AudioSourceMetadata(id = "yuntu-audio", name = "云图有声", host = "https://yuntuwechat.yuntuys.com")
object YunTuAudio : AudioSource by YunTuAudioSource()

internal class YunTuAudioSource(
    private val httpClient: HttpClient = HttpClientFactory.create(),
) : CachedAudioSource() {
    override val sourceId = "yuntu-audio"
    override val name = "云图有声"
    override val host = "https://yuntuwechat.yuntuys.com"
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaResolver = MediaResolverChain(httpClient)

    override suspend fun loadSearch(keyword: String): List<Book> =
        if (keyword.isBlank()) emptyList() else fetchBooks(keyword, SourceOperation.SEARCH)

    override suspend fun loadDetail(url: String): Book {
        val id = bookId(url) ?: throw parseError(SourceOperation.DETAIL, url, "detail:url.bookId")
        val keyword = detailKeyword(url) ?: throw parseError(SourceOperation.DETAIL, url, "detail:url.keyword")
        return fetchBooks(keyword, SourceOperation.DETAIL).firstOrNull { bookId(it.detailUrl) == id }
            ?: throw parseError(SourceOperation.DETAIL, url, "api:data.list[$id]")
    }

    override suspend fun loadChapters(url: String): List<Chapter> {
        val id = bookId(url) ?: throw parseError(SourceOperation.CHAPTERS, url, "chapters:url.bookId")
        val apiUrl = "https://open-service.yuntuys.com/api/w_ys/book/getChapters/wechat:$TENANT_ID/$id/true/asc?pageSize=5000&pageNum=1"
        val data = fetchRoot(apiUrl, SourceOperation.CHAPTERS).objectValue("data")
            ?: throw parseError(SourceOperation.CHAPTERS, apiUrl, "api:data")
        val units = data.objectValue("pageQuery")?.objectList("list").orEmpty()
        if (units.isEmpty()) throw parseError(SourceOperation.CHAPTERS, apiUrl, "api:data.pageQuery.list")
        return units.mapIndexedNotNull { fallbackIndex, unit ->
            val media = unit.textValue("audioUrl") ?: return@mapIndexedNotNull null
            Chapter(
                title = unit.textValue("name") ?: "第 ${fallbackIndex + 1} 集",
                url = media,
                index = unit.longValue("rank")?.minus(1)?.toInt() ?: fallbackIndex,
                durationMs = unit.longValue("audioDuration")?.times(1_000L),
            )
        }.also { if (it.isEmpty()) throw parseError(SourceOperation.CHAPTERS, apiUrl, "api:data.pageQuery.list.audioUrl") }
    }

    override suspend fun play(url: String): PlayInfo = mediaResolver.resolve(url)

    private suspend fun fetchBooks(keyword: String, operation: SourceOperation): List<Book> {
        val url = "https://open-service.yuntuys.com/api/w_ys/book/search/wechat:$TENANT_ID/${keyword.urlEncoded()}?pageSize=50&pageNum=1"
        val data = fetchRoot(url, operation).objectValue("data") ?: throw parseError(operation, url, "api:data")
        return data.objectList("list").mapNotNull { item ->
            val id = item.longValue("bookId") ?: return@mapNotNull null
            val title = item.textValue("bookName") ?: return@mapNotNull null
            Book(
                title = title,
                author = item.textValue("authorName"),
                narrator = item.textValue("anchorName"),
                coverUrl = item.textValue("cover"),
                category = item.textValue("classTypeName"),
                description = item.textValue("summary"),
                sourceId = sourceId,
                sourceName = name,
                detailUrl = "$host/home#/book/$id?keyword=${title.urlEncoded()}",
            )
        }
    }

    private suspend fun fetchRoot(url: String, operation: SourceOperation): JsonObject {
        val response = httpClient.execute(HttpRequest(url))
        if (response.code !in 200..299) throw httpError(operation, url, response.code)
        return runCatching { json.parseToJsonElement(response.text()).jsonObject }
            .getOrElse { throw SourceException(sourceId, operation, url, SourceErrorKind.PARSE, "api:json", "云图有声 JSON 无效", it) }
    }

    private fun bookId(url: String): Long? = Regex("/book/(\\d+)").find(url)?.groupValues?.get(1)?.toLongOrNull()

    private fun detailKeyword(url: String): String? = url.substringAfter("?keyword=", "")
        .substringBefore('&').takeIf { it.isNotBlank() }?.let { URLDecoder.decode(it, Charsets.UTF_8.name()) }

    private fun httpError(operation: SourceOperation, url: String, code: Int) =
        SourceException(sourceId, operation, url, SourceErrorKind.HTTP_STATUS, "http", "HTTP $code")

    private fun parseError(operation: SourceOperation, url: String?, location: String) =
        SourceException(sourceId, operation, url, SourceErrorKind.PARSE, location, "云图有声 API 字段缺失")

    private fun String.urlEncoded() = URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

    private companion object {
        const val TENANT_ID = "07955551-706c-4259-9aa0-db4627dfca57"
    }
}
