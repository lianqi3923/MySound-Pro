package io.github.mysoundpro.core.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpInfrastructurePresenceTest {
    @Test
    fun `shared http capabilities are available`() {
        val expectedMethods = mapOf(
            "io.github.mysoundpro.core.http.HttpClient" to setOf("execute"),
            "io.github.mysoundpro.core.http.OkHttpHttpClient" to setOf("execute"),
            "io.github.mysoundpro.core.http.RetryPolicy" to setOf("shouldRetry"),
            "io.github.mysoundpro.core.http.AnonymousCookieJar" to setOf("saveFromResponse", "loadForRequest"),
            "io.github.mysoundpro.core.http.CharsetDecoder" to setOf("decode"),
        )

        expectedMethods.forEach { (className, methods) ->
            val actual = Class.forName(className).methods.mapTo(mutableSetOf()) { it.name }
            assertThat(actual).containsAll(methods)
        }
    }
}
