package io.github.mysoundpro.host

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SourceEntryPresenceTest {
    @Test
    fun `exports the static entry points required by MyTingShu`() {
        // MyTingShu 2.6.0 derives the entry package from the JAR basename:
        // my_sound_pro.jar -> com.github.eprendre.my_sound_pro.SourceEntry.
        val entry = Class.forName("com.github.eprendre.my_sound_pro.SourceEntry")

        assertThat(entry.getMethod("getDesc").returnType).isEqualTo(String::class.java)
        assertThat(entry.getMethod("getCategory").returnType).isEqualTo(String::class.java)
        assertThat(entry.getMethod("getSources").returnType).isEqualTo(List::class.java)
    }
}
