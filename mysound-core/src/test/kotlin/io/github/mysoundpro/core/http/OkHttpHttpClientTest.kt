package io.github.mysoundpro.core.http

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class OkHttpHttpClientTest {
    private val server = MockWebServer()

    @AfterEach
    fun closeServer() {
        server.close()
    }

    @Test
    fun `retries recoverable response and sends user agent and referer`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        server.start()
        val delays = mutableListOf<Long>()
        val client = OkHttpHttpClient(
            callFactory = OkHttpClient(),
            retryPolicy = RetryPolicy(maxRetries = 2, initialDelayMs = 10L),
            sleeper = delays::add,
        )

        val response = client.execute(
            HttpRequest(
                url = server.url("/book").toString(),
                userAgent = "MySound-Test",
                referer = "https://public.example.test/",
            ),
        )

        assertThat(response.code).isEqualTo(200)
        assertThat(response.text()).isEqualTo("ok")
        assertThat(delays).containsExactly(10L)
        val first = server.takeRequest()
        assertThat(first.getHeader("User-Agent")).isEqualTo("MySound-Test")
        assertThat(first.getHeader("Referer")).isEqualTo("https://public.example.test/")
        server.takeRequest()
    }

    @Test
    fun `okhttp transparently decodes gzip`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Encoding", "gzip")
                .setBody(okio.Buffer().write(gzip("compressed"))),
        )
        server.start()
        val client = OkHttpHttpClient(OkHttpClient())

        val response = client.execute(HttpRequest(server.url("/gzip").toString()))

        assertThat(response.text()).isEqualTo("compressed")
    }

    private fun gzip(value: String): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(value.toByteArray()) }
        return output.toByteArray()
    }
}
