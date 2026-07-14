package io.github.mysoundpro.sources

import io.github.mysoundpro.api.MediaFormat
import io.github.mysoundpro.core.http.HttpClient
import io.github.mysoundpro.core.http.HttpResponse
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BookanAudioSourceTest {
    @Test
    fun `public api returns metadata all chapter pages and direct audio`() = runTest {
        val source = BookanAudioSource(HttpClient { request ->
            val fixture = when {
                "album/units" !in request.url -> "fixtures/bookan/search.json"
                "page=2" in request.url -> "fixtures/bookan/chapters-2.json"
                else -> "fixtures/bookan/chapters-1.json"
            }
            response(request.url, resource(fixture))
        })

        val found = source.search("三国演义").single()
        val detail = source.detail(found.detailUrl)
        val chapters = source.chapters(found.detailUrl)
        val play = source.play(chapters.first().url)

        assertThat(found.title).isEqualTo("三国演义")
        assertThat(found.author).isEqualTo("白云出岫、蓝色百合")
        assertThat(detail.description).contains("东汉末年")
        assertThat(chapters.map { it.index }).containsExactly(0, 1, 2)
        assertThat(chapters.last().title).contains("第三回")
        assertThat(play.format).isEqualTo(MediaFormat.AAC)
    }

    private fun response(url: String, body: String) = HttpResponse(
        200, url, mapOf("Content-Type" to listOf("application/json; charset=UTF-8")), body.toByteArray(),
    )

    private fun resource(path: String) = requireNotNull(javaClass.classLoader.getResource(path)).readText()
}
