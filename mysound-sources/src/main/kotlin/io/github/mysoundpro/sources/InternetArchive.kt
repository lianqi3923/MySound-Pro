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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup
import java.net.URLEncoder

/** Internet Archive 的公开有声书/诗歌集合 Parser。 */
/** 当前网络准入测试连接超时，因此仅保留 Parser，不注册到生产来源。 */
object InternetArchive : AudioSource by InternetArchiveSource()

internal class InternetArchiveSource(
    private val httpClient: HttpClient = HttpClientFactory.create(),
) : CachedAudioSource() {
    override val sourceId = "internet-archive"
    override val name = "Internet Archive 有声书"
    override val host = "https://archive.org"
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaResolver = MediaResolverChain(httpClient)

    override suspend fun loadSearch(keyword: String): List<Book> {
        if (keyword.isBlank()) return emptyList()
        val query = encode("collection:audio_bookspoetry AND title:($keyword)")
        val url = "$host/advancedsearch.php?q=$query&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=creator&fl%5B%5D=description&fl%5B%5D=language&fl%5B%5D=subject&rows=50&page=1&output=json"
        val root = fetchRoot(url, SourceOperation.SEARCH)
        val docs = ((root["response"] as? JsonObject)?.get("docs") as? JsonArray)
            ?: throw parseError(SourceOperation.SEARCH, url, "search:response.docs")
        return docs.mapNotNull { (it as? JsonObject)?.let(::parseSearchBook) }
    }

    override suspend fun loadDetail(url: String): Book {
        val root = fetchRoot(url, SourceOperation.DETAIL)
        val metadata = root["metadata"] as? JsonObject
            ?: throw parseError(SourceOperation.DETAIL, url, "metadata")
        return parseMetadataBook(metadata, url)
    }

    override suspend fun loadChapters(url: String): List<Chapter> {
        val root = fetchRoot(url, SourceOperation.CHAPTERS)
        val metadata = root["metadata"] as? JsonObject
            ?: throw parseError(SourceOperation.CHAPTERS, url, "metadata")
        val identifier = metadata.string("identifier")
            ?: throw parseError(SourceOperation.CHAPTERS, url, "metadata:identifier")
        val files = root["files"] as? JsonArray
            ?: throw parseError(SourceOperation.CHAPTERS, url, "files")
        val chapters = files.mapNotNull { it as? JsonObject }
            .filter { file -> file.string("name")?.substringBefore('?')?.lowercase()?.endsWith(".mp3") == true }
            .sortedBy { it.string("name") }
            .mapIndexed { index, file ->
                val fileName = requireNotNull(file.string("name"))
                Chapter(
                    title = file.string("title") ?: fileName.substringBeforeLast('.'),
                    url = "$host/download/${encodePath(identifier)}/${encodePath(fileName)}",
                    index = index,
                    durationMs = file.string("length")?.toDoubleOrNull()?.times(1_000)?.toLong(),
                )
            }
        if (chapters.isEmpty()) throw parseError(SourceOperation.CHAPTERS, url, "files:mp3")
        return chapters
    }

    override suspend fun play(url: String): PlayInfo = mediaResolver.resolve(url)

    private suspend fun fetchRoot(url: String, operation: SourceOperation): JsonObject {
        val response = httpClient.execute(HttpRequest(url))
        if (response.code !in 200..299) {
            throw SourceException(sourceId, operation, url, SourceErrorKind.HTTP_STATUS, "http", "HTTP ${response.code}")
        }
        return runCatching { json.parseToJsonElement(response.text()).jsonObject }
            .getOrElse { throw SourceException(sourceId, operation, url, SourceErrorKind.PARSE, "json", "Internet Archive JSON 无效", it) }
    }

    private fun parseSearchBook(doc: JsonObject): Book? {
        val identifier = doc.string("identifier") ?: return null
        return Book(
            title = doc.string("title") ?: identifier,
            author = doc.strings("creator").joinToString().ifBlank { null },
            coverUrl = "$host/services/img/$identifier",
            category = doc.strings("subject").joinToString().ifBlank { doc.strings("language").joinToString().ifBlank { null } },
            description = doc.strings("description").firstOrNull()?.let { Jsoup.parse(it).text() },
            sourceId = sourceId,
            sourceName = name,
            detailUrl = "$host/metadata/$identifier",
        )
    }

    private fun parseMetadataBook(metadata: JsonObject, url: String): Book {
        val identifier = metadata.string("identifier")
            ?: throw parseError(SourceOperation.DETAIL, url, "metadata:identifier")
        return Book(
            title = metadata.string("title") ?: identifier,
            author = metadata.strings("creator").joinToString().ifBlank { null },
            coverUrl = "$host/services/img/$identifier",
            category = metadata.strings("subject").joinToString().ifBlank { metadata.strings("language").joinToString().ifBlank { null } },
            description = metadata.strings("description").firstOrNull()?.let { Jsoup.parse(it).text() },
            sourceId = sourceId,
            sourceName = name,
            detailUrl = url,
        )
    }

    private fun JsonObject.string(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull?.ifBlank { null }
    private fun JsonObject.strings(name: String): List<String> = when (val value = this[name]) {
        is JsonPrimitive -> listOfNotNull(value.contentOrNull?.ifBlank { null })
        is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.ifBlank { null } }
        else -> emptyList()
    }
    private fun encode(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
    private fun encodePath(value: String) = encode(value).replace("%2F", "/", ignoreCase = true)
    private fun parseError(operation: SourceOperation, url: String?, location: String) =
        SourceException(sourceId, operation, url, SourceErrorKind.PARSE, location, "Internet Archive 字段缺失")
}
