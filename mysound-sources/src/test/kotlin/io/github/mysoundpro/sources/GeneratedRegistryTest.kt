package io.github.mysoundpro.sources

import io.github.mysoundpro.api.AudioSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GeneratedRegistryTest {
    @Test
    fun `ksp registry discovers fixture source without a manual list`() {
        val registryType = Class.forName("io.github.mysoundpro.generated.GeneratedSourceRegistry")
        val instance = registryType.getField("INSTANCE").get(null)
        @Suppress("UNCHECKED_CAST")
        val sources = registryType.getMethod("all").invoke(instance) as List<AudioSource>

        assertThat(sources.map { it.sourceId }).containsExactly("fixture-public")
        assertThat(sources.single().javaClass.simpleName).isEqualTo("FixtureAudioSource")
    }
}
