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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import java.net.URLEncoder

/** LibriVox 官方公开 API Parser，内容为志愿者录制的公版作品。 */
@AudioSourceMetadata(id = "librivox", name = "LibriVox 公版有声书", host = "https://librivox.org")
object LibriVox : AudioSource by LibriVoxSource()

internal class LibriVoxSource(
    private val httpClient: HttpClient = HttpClientFactory.create(),
) : CachedAudioSource() {
    override val sourceId = "librivox"
    override val name = "LibriVox 公版有声书"
    override val host = "https://librivox.org"
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaResolver = MediaResolverChain(httpClient)

    override suspend fun loadSearch(keyword: String): List<Book> {
        if (keyword.isBlank()) return emptyList()
        val encoded = URLEncoder.encode(keyword, Charsets.UTF_8.name()).replace("+", "%20")
        val url = "$host/api/feed/audiobooks/title/%5E$encoded?coverart=1&limit=50&format=json"
        return books(fetchRoot(url, SourceOperation.SEARCH), url, SourceOperation.SEARCH).map(::parseBook)
    }

    override suspend fun loadDetail(url: String): Book {
        val book = books(fetchRoot(url, SourceOperation.DETAIL), url, SourceOperation.DETAIL).firstOrNull()
            ?: throw parseError(SourceOperation.DETAIL, url, "api:books[0]")
        return parseBook(book)
    }

    override suspend fun loadChapters(url: String): List<Chapter> {
        val book = books(fetchRoot(url, SourceOperation.CHAPTERS), url, SourceOperation.CHAPTERS).firstOrNull()
            ?: throw parseError(SourceOperation.CHAPTERS, url, "api:books[0]")
        val sections = book["sections"] as? JsonArray
            ?: throw parseError(SourceOperation.CHAPTERS, url, "api:sections")
        return sections.mapIndexedNotNull { index, element ->
            val section = element as? JsonObject ?: return@mapIndexedNotNull null
            val mediaUrl = section.string("listen_url") ?: return@mapIndexedNotNull null
            Chapter(
                title = section.string("title") ?: "Section ${index + 1}",
                url = mediaUrl,
                index = section.string("section_number")?.toIntOrNull()?.minus(1) ?: index,
                durationMs = section.string("playtime")?.toLongOrNull()?.times(1_000L),
            )
        }.also {
            if (it.isEmpty()) throw parseError(SourceOperation.CHAPTERS, url, "api:sections.listen_url")
        }
    }

    override suspend fun play(url: String): PlayInfo = mediaResolver.resolve(url)

    private suspend fun fetchRoot(url: String, operation: SourceOperation): JsonObject {
        val response = httpClient.execute(HttpRequest(url))
        if (response.code !in 200..299) {
            throw SourceException(sourceId, operation, url, SourceErrorKind.HTTP_STATUS, "http", "HTTP ${response.code}")
        }
        return runCatching { json.parseToJsonElement(response.text()).jsonObject }
            .getOrElse { throw SourceException(sourceId, operation, url, SourceErrorKind.PARSE, "api:json", "LibriVox JSON 无效", it) }
    }

    private fun books(root: JsonObject, url: String, operation: SourceOperation): List<JsonObject> {
        val array = root["books"] as? JsonArray ?: throw parseError(operation, url, "api:books")
        return array.mapNotNull { it as? JsonObject }
    }

    private fun parseBook(book: JsonObject): Book {
        val id = book.string("id") ?: throw parseError(SourceOperation.DETAIL, null, "api:book.id")
        val authors = (book["authors"] as? JsonArray).orEmpty().mapNotNull { author ->
            (author as? JsonObject)?.let { listOfNotNull(it.string("first_name"), it.string("last_name")).joinToString(" ").ifBlank { null } }
        }
        val narrators = (book["sections"] as? JsonArray).orEmpty().flatMap { section ->
            ((section as? JsonObject)?.get("readers") as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.string("display_name") }
        }.distinct()
        return Book(
            title = book.string("title") ?: throw parseError(SourceOperation.DETAIL, null, "api:book.title"),
            author = authors.joinToString().ifBlank { null },
            narrator = narrators.joinToString().ifBlank { null },
            coverUrl = book.string("coverart_jpg") ?: book.string("coverart_thumbnail"),
            category = book.string("language"),
            description = book.string("description")?.let { Jsoup.parse(it).text() },
            sourceId = sourceId,
            sourceName = name,
            detailUrl = "$host/api/feed/audiobooks/?id=$id&extended=1&coverart=1&format=json",
        )
    }

    private fun JsonObject.string(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull?.ifBlank { null }

    private fun parseError(operation: SourceOperation, url: String?, location: String) =
        SourceException(sourceId, operation, url, SourceErrorKind.PARSE, location, "LibriVox API 字段缺失")
}
