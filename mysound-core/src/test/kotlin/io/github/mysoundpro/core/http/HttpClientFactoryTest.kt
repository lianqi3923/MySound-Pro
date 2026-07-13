package io.github.mysoundpro.core.http

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpClientFactoryTest {
    @Test
    fun `default client advertises and decodes brotli`() = runTest {
        val compressed = (
            "1bce00009c05ceb9f028d14e416230f718960a537b0922d2f7b6adef56532c08dff44551516690131494db" +
                "6021c7e3616c82c1bc2416abb919aaa06e8d30d82cc2981c2f5c900bfb8ee29d5c03deb1c0dacff80e" +
                "abe82ba64ed250a497162006824684db917963ecebe041b352a3e62d629cc97b95cac24265b175171e" +
                "5cb384cd0912aeb5b5dd9555f2dd1a9b20688201"
            ).decodeHex()
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Encoding", "br")
                .setBody(Buffer().write(compressed)),
        )
        server.start()
        try {
            val response = HttpClientFactory.create().execute(HttpRequest(server.url("/brotli").toString()))

            assertThat(response.text()).contains("\"brotli\": true")
            assertThat(server.takeRequest().getHeader("Accept-Encoding")).isEqualTo("br,gzip")
        } finally {
            server.shutdown()
        }
    }
}
