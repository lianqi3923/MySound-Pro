package io.github.mysoundpro.sources

import io.github.mysoundpro.api.MediaFormat
import io.github.mysoundpro.core.http.HttpClient
import io.github.mysoundpro.core.http.HttpResponse
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YunTuAudioSourceTest {
    @Test
    fun `public api returns searchable chinese catalog and mp3 chapters`() = runTest {
        val source = YunTuAudioSource(HttpClient { request ->
            val fixture = if ("getChapters" in request.url) {
                "fixtures/yuntu/chapters.json"
            } else {
                "fixtures/yuntu/search.json"
            }
            response(request.url, resource(fixture))
        })

        val found = source.search("三国演义").single()
        val detail = source.detail(found.detailUrl)
        val chapters = source.chapters(found.detailUrl)
        val play = source.play(chapters.first().url)

        assertThat(found.author).isEqualTo("罗贯中")
        assertThat(found.narrator).isEqualTo("白云出岫")
        assertThat(detail.description).contains("三国鼎立")
        assertThat(chapters.map { it.durationMs }).containsExactly(1_670_000L, 1_986_000L)
        assertThat(play.format).isEqualTo(MediaFormat.MP3)
    }

    private fun response(url: String, body: String) = HttpResponse(
        200, url, mapOf("Content-Type" to listOf("application/json; charset=UTF-8")), body.toByteArray(),
    )

    private fun resource(path: String) = requireNotNull(javaClass.classLoader.getResource(path)).readText()
}
