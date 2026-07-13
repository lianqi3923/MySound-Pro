package io.github.mysoundpro.sources

import io.github.mysoundpro.api.MediaFormat
import io.github.mysoundpro.api.SourceErrorKind
import io.github.mysoundpro.api.SourceException
import io.github.mysoundpro.core.http.HttpClient
import io.github.mysoundpro.core.http.HttpResponse
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GutenbergAudioSourceTest {
    @Test
    fun `public sound catalog and Gutenberg MP3 chapters satisfy contract`() = runTest {
        val source = GutenbergAudioSource(HttpClient { request ->
            if (request.url.endsWith(".html")) html(request.url) else json(request.url, "fixtures/gutenberg/search.json")
        })

        val found = source.search("Alice").single()
        val detail = source.detail(found.detailUrl)
        val chapters = source.chapters(found.detailUrl)
        val play = source.play(chapters.first().url)

        assertThat(found.author).isEqualTo("Carroll, Lewis")
        assertThat(found.category).contains("Children's Literature")
        assertThat(detail.description).isEqualTo("A public-domain classic.")
        assertThat(chapters.map { it.title }).containsExactly("Chapter I: Down the Rabbit-Hole", "Chapter II: The Pool of Tears")
        assertThat(play.format).isEqualTo(MediaFormat.MP3)
    }

    @Test
    fun `invalid catalog response reports parser location`() = runTest {
        val source = GutenbergAudioSource(HttpClient { json(it.url, "fixtures/gutenberg/error.json") })

        val error = runCatching { source.detail("https://gutendex.com/books/0") }.exceptionOrNull()

        assertThat(error).isInstanceOf(SourceException::class.java)
        assertThat((error as SourceException).kind).isEqualTo(SourceErrorKind.PARSE)
        assertThat(error.location).isEqualTo("catalog:book")
    }

    private fun json(url: String, fixture: String) = HttpResponse(
        200, url, mapOf("Content-Type" to listOf("application/json; charset=UTF-8")), resource(fixture).toByteArray(),
    )
    private fun html(url: String) = HttpResponse(
        200, url, mapOf("Content-Type" to listOf("text/html; charset=UTF-8")), resource("fixtures/gutenberg/chapters.html").toByteArray(),
    )
    private fun resource(path: String) = requireNotNull(javaClass.classLoader.getResource(path)).readText()
}
