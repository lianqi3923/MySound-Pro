package io.github.mysoundpro.sources

import io.github.mysoundpro.api.MediaFormat
import io.github.mysoundpro.api.SourceErrorKind
import io.github.mysoundpro.api.SourceException
import io.github.mysoundpro.core.http.HttpClient
import io.github.mysoundpro.core.http.HttpResponse
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class Ting69SourceTest {
    @Test
    fun `search detail chapters and play satisfy source contract`() = runTest {
        val source = Ting69Source(fixtureClient())

        val found = source.search("黄金瞳").single()
        val detail = source.detail(found.detailUrl)
        val chapters = source.chapters(found.detailUrl)
        val play = source.play(chapters.first().url)

        assertThat(found.author).isEqualTo("打眼")
        assertThat(found.narrator).isEqualTo("郭益达")
        assertThat(found.coverUrl).isEqualTo("https://www.ting69.com/covers/gold.jpg")
        assertThat(detail.description).contains("典当行")
        assertThat(chapters.map { it.index }).containsExactly(0, 1)
        assertThat(play.format).isEqualTo(MediaFormat.AAC)
        assertThat(play.headers).containsKey("Referer")
    }

    @Test
    fun `error page reports structured parser location`() = runTest {
        val source = Ting69Source(HttpClient { response(it.url, resource("fixtures/ting69/error.html")) })

        val error = runCatching { source.detail("https://www.ting69.com/show/broken.html") }.exceptionOrNull()

        assertThat(error).isInstanceOf(SourceException::class.java)
        assertThat((error as SourceException).kind).isEqualTo(SourceErrorKind.PARSE)
        assertThat(error.location).isEqualTo("detail:h1")
    }

    private fun fixtureClient() = HttpClient { request ->
        val fixture = when {
            request.url.contains("search.php") -> "fixtures/ting69/search.html"
            request.url.contains("/show/") -> "fixtures/ting69/detail.html"
            else -> "fixtures/ting69/play.html"
        }
        response(request.url, resource(fixture))
    }

    private fun response(url: String, body: String) = HttpResponse(
        200,
        url,
        mapOf("Content-Type" to listOf("text/html; charset=UTF-8")),
        body.toByteArray(),
    )
    private fun resource(path: String) = requireNotNull(javaClass.classLoader.getResource(path)).readText()
}
