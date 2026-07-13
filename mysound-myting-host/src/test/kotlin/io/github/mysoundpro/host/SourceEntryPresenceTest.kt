package io.github.mysoundpro.host

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SourceEntryPresenceTest {
    @Test
    fun `exports the static entry points required by MyTingShu`() {
        val entry = Class.forName("com.github.eprendre.sources_by_mysound_pro.SourceEntry")

        assertThat(entry.getMethod("getDesc").returnType).isEqualTo(String::class.java)
        assertThat(entry.getMethod("getCategory").returnType).isEqualTo(String::class.java)
        assertThat(entry.getMethod("getSources").returnType).isEqualTo(List::class.java)
    }
}
