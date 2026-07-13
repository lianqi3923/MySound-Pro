package io.github.mysoundpro.core.http

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AnonymousCookieJarTest {
    @Test
    fun `anonymous cookies are returned only to matching hosts`() {
        val jar = AnonymousCookieJar()
        val origin = "https://audio.example.test/book".toHttpUrl()
        val cookie = Cookie.parse(origin, "visitor=public; Path=/; HttpOnly")!!

        jar.saveFromResponse(origin, listOf(cookie))

        assertThat(jar.loadForRequest("https://audio.example.test/chapter".toHttpUrl()))
            .extracting<String> { it.name }
            .containsExactly("visitor")
        assertThat(jar.loadForRequest("https://other.example.test/".toHttpUrl())).isEmpty()
    }
}
