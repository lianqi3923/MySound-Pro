package io.github.mysoundpro.sources

import io.github.mysoundpro.core.http.HttpClientFactory
import io.github.mysoundpro.core.http.HttpRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.random.Random

/**
 * 显式启用的低频线上可靠性测试。默认构建不会访问外网：
 * `./gradlew :mysound-sources:test -Pmysound.live=true`。
 */
class LiveSourceReliabilityTest {
    private val http = HttpClientFactory.create()

    @Test
    fun `random 100 public books produce a success rate report`() = runBlocking {
        requireLive()
        val sampleSize = System.getProperty("mysound.live.sampleSize", "100").toInt()
        val gutenbergCandidates = gutenbergCandidates()
        val libriCandidates = libriVoxCandidates()
        val gutenbergCount = minOf(30, sampleSize / 2)
        val samples = gutenbergCandidates.shuffled(Random(20260714)).take(gutenbergCount).map { GutenbergAudioSource() to it } +
            libriCandidates.shuffled(Random(20260714)).take(sampleSize - gutenbergCount).map { LibriVoxSource() to it }
        assertThat(samples).hasSize(sampleSize)

        val results = samples.map { (source, url) ->
            delay(250L)
            val error = runCatching {
                val book = source.detail(url)
                check(book.title.isNotBlank())
                check(book.detailUrl.isNotBlank())
            }.exceptionOrNull()
            LiveResult(source.sourceId, url, error == null, error?.message)
        }
        val success = results.count(LiveResult::success)
        writeReport(sampleSize, success, results)

        assertThat(success.toDouble() / sampleSize).isGreaterThanOrEqualTo(0.85)
        Unit
    }

    @Test
    fun `search chapter and play smoke works for all live sources`() = runBlocking {
        requireLive()
        val gutenbergBook = GutenbergAudioSource().search("Alice").first()
        val gutenbergPlay = GutenbergAudioSource().chapters(gutenbergBook.detailUrl).first().let { GutenbergAudioSource().play(it.url) }
        val libriBook = LibriVoxSource().search("Alice").first()
        val libriPlay = LibriVoxSource().chapters(libriBook.detailUrl).first().let { LibriVoxSource().play(it.url) }
        val tingShuWangBook = TingShuWangSource().search("流浪地球").first()
        val tingShuWangPlay = TingShuWangSource().chapters(tingShuWangBook.detailUrl).first().let { TingShuWangSource().play(it.url) }
        val bookanBook = BookanAudioSource().search("三国演义").first()
        val bookanPlay = BookanAudioSource().chapters(bookanBook.detailUrl).first().let { BookanAudioSource().play(it.url) }
        val yunTuBook = YunTuAudioSource().search("三国演义").first()
        val yunTuPlay = YunTuAudioSource().chapters(yunTuBook.detailUrl).first().let { YunTuAudioSource().play(it.url) }

        assertThat(gutenbergPlay.url).matches("https?://.+\\.mp3(\\?.*)?")
        assertThat(libriPlay.url).contains(".mp3")
        assertThat(tingShuWangPlay.url).contains(".mp3")
        assertThat(bookanPlay.url).matches("https?://.+\\.(m4a|aac)(\\?.*)?")
        assertThat(yunTuPlay.url).contains(".mp3")
        Unit
    }

    private suspend fun gutenbergCandidates(): List<String> {
        val url = "https://gutendex.com/books/?mime_type=audio%2Fmpeg&page=1"
        val root = Json.parseToJsonElement(http.execute(HttpRequest(url)).text()).jsonObject
        return root.getValue("results").jsonArray.map { element ->
            val id = element.jsonObject.getValue("id").jsonPrimitive.content
            "https://gutendex.com/books/$id"
        }
    }

    private suspend fun libriVoxCandidates(): List<String> {
        val url = "https://librivox.org/api/feed/audiobooks/?limit=100&offset=200&format=json"
        val root = Json.parseToJsonElement(http.execute(HttpRequest(url)).text()).jsonObject
        return root.getValue("books").jsonArray.map { element ->
            val id = element.jsonObject.getValue("id").jsonPrimitive.content
            "https://librivox.org/api/feed/audiobooks/?id=$id&extended=1&coverart=1&format=json"
        }
    }

    private fun writeReport(sampleSize: Int, success: Int, results: List<LiveResult>) {
        val failures = results.filterNot(LiveResult::success)
        val json = buildString {
            appendLine("{")
            appendLine("  \"sampleSize\": $sampleSize,")
            appendLine("  \"success\": $success,")
            appendLine("  \"failure\": ${sampleSize - success},")
            appendLine("  \"successRate\": ${"%.4f".format(success.toDouble() / sampleSize)},")
            appendLine("  \"failures\": [")
            failures.forEachIndexed { index, item ->
                val comma = if (index == failures.lastIndex) "" else ","
                appendLine("    {\"source\":\"${item.source}\",\"url\":\"${item.url}\",\"reason\":\"${escape(item.reason.orEmpty())}\"}$comma")
            }
            appendLine("  ]")
            appendLine("}")
        }
        File("build/reports/live/random-100.json").apply { parentFile.mkdirs(); writeText(json) }
    }

    private fun escape(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
    private fun requireLive() = assumeTrue(System.getProperty("mysound.live") == "true", "live tests are opt-in")
    private data class LiveResult(val source: String, val url: String, val success: Boolean, val reason: String?)
}
