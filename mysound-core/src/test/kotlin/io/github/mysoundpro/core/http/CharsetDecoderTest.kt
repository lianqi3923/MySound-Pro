package io.github.mysoundpro.core.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.Charset

class CharsetDecoderTest {
    @Test
    fun `http charset takes precedence over fallback`() {
        val bytes = "中文内容".toByteArray(Charset.forName("GB18030"))

        val decoded = CharsetDecoder.decode(bytes, "text/html; charset=GB18030", "UTF-8")

        assertThat(decoded).isEqualTo("中文内容")
    }

    @Test
    fun `html meta charset is detected when header omits it`() {
        val html = "<html><head><meta charset=GB18030></head><body>中文</body></html>"
        val bytes = html.toByteArray(Charset.forName("GB18030"))

        val decoded = CharsetDecoder.decode(bytes, "text/html", "UTF-8")

        assertThat(decoded).contains("中文")
    }
}
