package io.github.mysoundpro.sources

import io.github.mysoundpro.api.MediaFormat
import io.github.mysoundpro.api.SourceErrorKind
import io.github.mysoundpro.api.SourceException
import io.github.mysoundpro.core.http.HttpClient
import io.github.mysoundpro.core.http.HttpResponse
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InternetArchiveSourceTest {
    @Test
    fun `public archive search detail chapters and direct play satisfy contract`() = runTest {
        val source = InternetArchiveSource(HttpClient { request ->
            val fixture = if (request.url.contains("advancedsearch")) "fixtures/internetarchive/search.json" else "fixtures/internetarchive/detail.json"
            response(request.url, resource(fixture))
        })

        val found = source.search("Alice").single()
        val detail = source.detail(found.detailUrl)
        val chapters = source.chapters(found.detailUrl)
        val play = source.play(chapters.single().url)

        assertThat(found.author).isEqualTo("Lewis Carroll")
        assertThat(found.category).contains("Audiobook")
        assertThat(detail.coverUrl).isEqualTo("https://archive.org/services/img/alice_in_wonderland_audio")
        assertThat(chapters.single().title).isEqualTo("Down the Rabbit-Hole")
        assertThat(chapters.single().durationMs).isEqualTo(612_500L)
        assertThat(play.format).isEqualTo(MediaFormat.MP3)
    }

    @Test
    fun `invalid archive metadata reports parser location`() = runTest {
        val source = InternetArchiveSource(HttpClient { response(it.url, resource("fixtures/internetarchive/error.json")) })

        val error = runCatching { source.detail("https://archive.org/metadata/broken") }.exceptionOrNull()

        assertThat(error).isInstanceOf(SourceException::class.java)
        assertThat((error as SourceException).kind).isEqualTo(SourceErrorKind.PARSE)
        assertThat(error.location).isEqualTo("metadata:identifier")
    }

    private fun response(url: String, body: String) = HttpResponse(
        200,
        url,
        mapOf("Content-Type" to listOf("application/json; charset=UTF-8")),
        body.toByteArray(),
    )
    private fun resource(path: String) = requireNotNull(javaClass.classLoader.getResource(path)).readText()
}
