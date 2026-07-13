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
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup
import java.net.URLEncoder

/** Gutendex 公开目录 + Project Gutenberg 官方公开音频。 */
@AudioSourceMetadata(id = "gutenberg-audio", name = "Project Gutenberg 音频", host = "https://gutendex.com")
object GutenbergAudio : AudioSource by GutenbergAudioSource()

internal class GutenbergAudioSource(
    private val httpClient: HttpClient = HttpClientFactory.create(),
) : CachedAudioSource() {
    override val sourceId = "gutenberg-audio"
    override val name = "Project Gutenberg 音频"
    override val host = "https://gutendex.com"
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaResolver = MediaResolverChain(httpClient)

    override suspend fun loadSearch(keyword: String): List<Book> {
        if (keyword.isBlank()) return emptyList()
        val url = "$host/books/?mime_type=audio%2Fmpeg&search=${encode(keyword)}"
        val root = fetchRoot(url, SourceOperation.SEARCH)
        val results = root["results"] as? JsonArray
            ?: throw parseError(SourceOperation.SEARCH, url, "catalog:results")
        return results.mapNotNull { (it as? JsonObject)?.let(::parseBook) }
    }

    override suspend fun loadDetail(url: String): Book {
        val catalogBook = catalogBook(fetchRoot(url, SourceOperation.DETAIL))
            ?: throw parseError(SourceOperation.DETAIL, url, "catalog:book")
        return parseBook(catalogBook) ?: throw parseError(SourceOperation.DETAIL, url, "catalog:book")
    }

    override suspend fun loadChapters(url: String): List<Chapter> {
        val catalogBook = catalogBook(fetchRoot(url, SourceOperation.CHAPTERS))
            ?: throw parseError(SourceOperation.CHAPTERS, url, "catalog:book")
        val formats = catalogBook["formats"] as? JsonObject
            ?: throw parseError(SourceOperation.CHAPTERS, url, "catalog:formats")
        val indexUrl = formats.string("text/html")
            ?: throw parseError(SourceOperation.CHAPTERS, url, "catalog:formats.text/html")
        val response = httpClient.execute(HttpRequest(indexUrl))
        if (response.code !in 200..299) {
            throw SourceException(sourceId, SourceOperation.CHAPTERS, indexUrl, SourceErrorKind.HTTP_STATUS, "index:http", "HTTP ${response.code}")
        }
        val document = Jsoup.parse(response.text(), response.finalUrl)
        val chapters = document.select("a[href]").asSequence()
            .filter { it.attr("href").substringBefore('?').lowercase().endsWith(".mp3") }
            .distinctBy { it.absUrl("href") }
            .mapIndexed { index, link ->
                Chapter(
                    title = link.text().trim().ifBlank { link.attr("href").substringAfterLast('/').substringBeforeLast('.') },
                    url = link.absUrl("href"),
                    index = index,
                )
            }.toList()
        if (chapters.isEmpty()) throw parseError(SourceOperation.CHAPTERS, indexUrl, "index:a[href=.mp3]")
        return chapters
    }

    override suspend fun play(url: String): PlayInfo = mediaResolver.resolve(url)

    private suspend fun fetchRoot(url: String, operation: SourceOperation): JsonObject {
        val response = httpClient.execute(HttpRequest(url))
        if (response.code !in 200..299) {
            throw SourceException(sourceId, operation, url, SourceErrorKind.HTTP_STATUS, "http", "HTTP ${response.code}")
        }
        return runCatching { json.parseToJsonElement(response.text()).jsonObject }
            .getOrElse { throw SourceException(sourceId, operation, url, SourceErrorKind.PARSE, "catalog:json", "Gutendex JSON 无效", it) }
    }

    private fun catalogBook(root: JsonObject): JsonObject? =
        (root["results"] as? JsonArray)?.firstOrNull() as? JsonObject ?: root.takeIf { "id" in it }

    private fun parseBook(book: JsonObject): Book? {
        val id = (book["id"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: return null
        val formats = book["formats"] as? JsonObject ?: return null
        if (formats.string("audio/mpeg") == null) return null
        val authors = (book["authors"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.string("name") }
        val shelves = book.strings("bookshelves")
        val subjects = book.strings("subjects")
        return Book(
            title = book.string("title") ?: "Gutenberg $id",
            author = authors.joinToString().ifBlank { null },
            coverUrl = formats.string("image/jpeg"),
            category = (shelves + subjects.take(2)).joinToString().ifBlank { null },
            description = book.strings("summaries").firstOrNull(),
            sourceId = sourceId,
            sourceName = name,
            detailUrl = "$host/books/$id",
        )
    }

    private fun JsonObject.string(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull?.ifBlank { null }
    private fun JsonObject.strings(name: String): List<String> = (this[name] as? JsonArray).orEmpty()
        .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.ifBlank { null } }
    private fun encode(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
    private fun parseError(operation: SourceOperation, url: String?, location: String) =
        SourceException(sourceId, operation, url, SourceErrorKind.PARSE, location, "Gutendex/Gutenberg 字段缺失")
}
