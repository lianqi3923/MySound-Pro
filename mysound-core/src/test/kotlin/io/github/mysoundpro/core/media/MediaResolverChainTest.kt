package io.github.mysoundpro.core.media

import io.github.mysoundpro.api.MediaFormat
import io.github.mysoundpro.core.http.HttpClient
import io.github.mysoundpro.core.http.HttpRequest
import io.github.mysoundpro.core.http.HttpResponse
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MediaResolverChainTest {
    @Test
    fun `direct media URL is returned without a network request`() = runTest {
        var requests = 0
        val chain = MediaResolverChain(HttpClient {
            requests++
            error("must not fetch direct media")
        })

        val result = chain.resolve("https://cdn.example.test/book/01.mp3")

        assertThat(result.url).isEqualTo("https://cdn.example.test/book/01.mp3")
        assertThat(result.format).isEqualTo(MediaFormat.MP3)
        assertThat(requests).isZero()
    }

    @Test
    fun `resolves DOM JSON static script and iframe media candidates`() = runTest {
        val pages = mapOf(
            "https://site.test/dom" to """<audio><source src="/a/01.aac"></audio>""",
            "https://site.test/json" to """<script type="application/json">{"audio":{"url":"https://cdn.test/02.m3u8"}}</script>""",
            "https://site.test/script" to """<script>window.player = { url: 'https://cdn.test/03.mp3' };</script>""",
            "https://site.test/frame" to """<iframe src="/embedded/player"></iframe>""",
            "https://site.test/embedded/player" to """<audio src="https://cdn.test/04.mp3"></audio>""",
        )
        val chain = MediaResolverChain(fakeClient(pages))

        assertThat(chain.resolve("https://site.test/dom").url).isEqualTo("https://site.test/a/01.aac")
        assertThat(chain.resolve("https://site.test/json").format).isEqualTo(MediaFormat.M3U8)
        assertThat(chain.resolve("https://site.test/script").url).isEqualTo("https://cdn.test/03.mp3")
        assertThat(chain.resolve("https://site.test/frame").url).isEqualTo("https://cdn.test/04.mp3")
    }

    @Test
    fun `resolved page media carries public referer header`() = runTest {
        val chain = MediaResolverChain(fakeClient(mapOf(
            "https://site.test/play/1" to """<audio src="https://cdn.test/1.mp3"></audio>""",
        )))

        val result = chain.resolve("https://site.test/play/1")

        assertThat(result.headers).containsEntry("Referer", "https://site.test/play/1")
    }

    private fun fakeClient(pages: Map<String, String>) = HttpClient { request: HttpRequest ->
        val body = pages[request.url] ?: error("unexpected URL ${request.url}")
        HttpResponse(200, request.url, mapOf("Content-Type" to listOf("text/html; charset=UTF-8")), body.toByteArray())
    }
}
