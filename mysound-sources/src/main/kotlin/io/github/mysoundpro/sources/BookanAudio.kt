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

/** 博看有声公开目录 API Parser；instanceId 是公开租户编号，不是用户凭据。 */
@AudioSourceMetadata(id = "bookan-audio", name = "博看有声", host = "https://voicewk.bookan.com.cn")
object BookanAudio : AudioSource by BookanAudioSource()

internal class BookanAudioSource(
    private val httpClient: HttpClient = HttpClientFactory.create(),
) : CachedAudioSource() {
    override val sourceId = "bookan-audio"
    override val name = "博看有声"
    override val host = "https://voicewk.bookan.com.cn"
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
        val first = fetchChapterPage(id, 1)
        val pages = buildList {
            addAll(first.objectList("list"))
            val lastPage = first.longValue("last_page")?.toInt()?.coerceAtLeast(1) ?: 1
            for (page in 2..lastPage) addAll(fetchChapterPage(id, page).objectList("list"))
        }
        if (pages.isEmpty()) throw parseError(SourceOperation.CHAPTERS, url, "api:data.list")
        return pages.mapIndexedNotNull { index, unit ->
            val media = unit.textValue("file") ?: return@mapIndexedNotNull null
            Chapter(
                title = unit.textValue("title") ?: "第 ${index + 1} 集",
                url = media,
                index = index,
                durationMs = unit.longValue("duration")?.times(1_000L),
            )
        }.also { if (it.isEmpty()) throw parseError(SourceOperation.CHAPTERS, url, "api:data.list.file") }
    }

    override suspend fun play(url: String): PlayInfo = mediaResolver.resolve(url)

    private suspend fun fetchBooks(keyword: String, operation: SourceOperation): List<Book> {
        val url = "https://es.bookan.com.cn/api/v3/voice/book?instanceId=$INSTANCE_ID&keyword=${keyword.urlEncoded()}&pageNum=1&limitNum=50"
        val data = fetchRoot(url, operation).objectValue("data") ?: throw parseError(operation, url, "api:data")
        return data.objectList("list").mapNotNull { item ->
            val id = item.longValue("id") ?: return@mapNotNull null
            val title = item.textValue("name") ?: return@mapNotNull null
            Book(
                title = title,
                author = item.objectValue("extra")?.textValue("author"),
                coverUrl = item.textValue("cover"),
                category = item.textValue("press"),
                description = item.textValue("intro"),
                sourceId = sourceId,
                sourceName = name,
                detailUrl = "$host/$INSTANCE_WEB/index#/book/$id?keyword=${title.urlEncoded()}",
            )
        }
    }

    private suspend fun fetchChapterPage(id: Long, page: Int): JsonObject {
        val url = "https://api.bookan.com.cn/voice/album/units?album_id=$id&page=$page&num=200&order=1"
        return fetchRoot(url, SourceOperation.CHAPTERS).objectValue("data")
            ?: throw parseError(SourceOperation.CHAPTERS, url, "api:data")
    }

    private suspend fun fetchRoot(url: String, operation: SourceOperation): JsonObject {
        val response = httpClient.execute(HttpRequest(url))
        if (response.code !in 200..299) throw httpError(operation, url, response.code)
        return runCatching { json.parseToJsonElement(response.text()).jsonObject }
            .getOrElse { throw SourceException(sourceId, operation, url, SourceErrorKind.PARSE, "api:json", "博看有声 JSON 无效", it) }
    }

    private fun bookId(url: String): Long? = Regex("/book/(\\d+)").find(url)?.groupValues?.get(1)?.toLongOrNull()

    private fun detailKeyword(url: String): String? = url.substringAfter("?keyword=", "")
        .substringBefore('&').takeIf { it.isNotBlank() }?.let { URLDecoder.decode(it, Charsets.UTF_8.name()) }

    private fun httpError(operation: SourceOperation, url: String, code: Int) =
        SourceException(sourceId, operation, url, SourceErrorKind.HTTP_STATUS, "http", "HTTP $code")

    private fun parseError(operation: SourceOperation, url: String?, location: String) =
        SourceException(sourceId, operation, url, SourceErrorKind.PARSE, location, "博看有声 API 字段缺失")

    private fun String.urlEncoded() = URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

    private companion object {
        const val INSTANCE_ID = 25304
        const val INSTANCE_WEB = 25303
    }
}
