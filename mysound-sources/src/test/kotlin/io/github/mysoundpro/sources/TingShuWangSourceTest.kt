package io.github.mysoundpro.sources

import io.github.mysoundpro.api.MediaFormat
import io.github.mysoundpro.core.http.HttpClient
import io.github.mysoundpro.core.http.HttpResponse
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TingShuWangSourceTest {
    @Test
    fun `public html supports complete listening flow`() = runTest {
        val source = TingShuWangSource(fixtureClient())

        val found = source.search("流浪地球").single()
        val detail = source.detail(found.detailUrl)
        val chapters = source.chapters(found.detailUrl)
        val play = source.play(chapters.first().url)

        assertThat(found.author).isEqualTo("刘慈欣")
        assertThat(found.narrator).isEqualTo("类星体剧场")
        assertThat(detail.description).contains("硬核科幻")
        assertThat(chapters.map { it.title }).containsExactly("第01集-刹车时代", "第02集-逃逸时代")
        assertThat(chapters.map { it.index }).containsExactly(0, 1)
        assertThat(play.url).isEqualTo("https://media.example/lldq-001.mp3")
        assertThat(play.format).isEqualTo(MediaFormat.MP3)
        assertThat(play.headers["Referer"]).isEqualTo(chapters.first().url)
    }

    private fun fixtureClient() = HttpClient { request ->
        val fixture = when {
            "/so/" in request.url -> "fixtures/tingshuwang/search.html"
            request.url.endsWith("/book/LiuLangDeQiu/") -> "fixtures/tingshuwang/detail.html"
            else -> "fixtures/tingshuwang/play.html"
        }
        response(request.url, resource(fixture))
    }

    private fun response(url: String, body: String) = HttpResponse(
        200, url, mapOf("Content-Type" to listOf("text/html; charset=UTF-8")), body.toByteArray(),
    )

    private fun resource(path: String) = requireNotNull(javaClass.classLoader.getResource(path)).readText()
}
