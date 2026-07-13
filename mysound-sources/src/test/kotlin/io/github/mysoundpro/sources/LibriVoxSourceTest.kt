package io.github.mysoundpro.sources

import io.github.mysoundpro.api.MediaFormat
import io.github.mysoundpro.api.SourceErrorKind
import io.github.mysoundpro.api.SourceException
import io.github.mysoundpro.core.http.HttpClient
import io.github.mysoundpro.core.http.HttpResponse
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LibriVoxSourceTest {
    @Test
    fun `official API search detail chapters and direct play satisfy contract`() = runTest {
        val requested = mutableListOf<String>()
        val source = LibriVoxSource(HttpClient { request ->
            requested += request.url
            val fixture = if (request.url.contains("extended=1")) "fixtures/librivox/detail.json" else "fixtures/librivox/search.json"
            response(request.url, resource(fixture))
        })

        val found = source.search("Letters").single()
        val detail = source.detail(found.detailUrl)
        val chapters = source.chapters(found.detailUrl)
        val play = source.play(chapters.single().url)

        assertThat(found.author).isEqualTo("Honoré de Balzac")
        assertThat(detail.narrator).isEqualTo("Kara Shallenberg")
        assertThat(detail.category).isEqualTo("English")
        assertThat(chapters.single().durationMs).isEqualTo(1_764_000L)
        assertThat(play.format).isEqualTo(MediaFormat.MP3)
        assertThat(requested.first()).contains("/title/%5ELetters")
    }

    @Test
    fun `invalid API response reports structured parser location`() = runTest {
        val source = LibriVoxSource(HttpClient { response(it.url, resource("fixtures/librivox/error.json")) })

        val error = runCatching { source.search("missing") }.exceptionOrNull()

        assertThat(error).isInstanceOf(SourceException::class.java)
        assertThat((error as SourceException).kind).isEqualTo(SourceErrorKind.PARSE)
        assertThat(error.location).isEqualTo("api:books")
    }

    private fun response(url: String, body: String) = HttpResponse(
        code = 200,
        finalUrl = url,
        headers = mapOf("Content-Type" to listOf("application/json; charset=UTF-8")),
        body = body.toByteArray(),
    )
    private fun resource(path: String) = requireNotNull(javaClass.classLoader.getResource(path)).readText()
}
