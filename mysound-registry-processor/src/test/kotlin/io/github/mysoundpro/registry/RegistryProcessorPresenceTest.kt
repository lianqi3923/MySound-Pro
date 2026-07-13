package io.github.mysoundpro.registry

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RegistryProcessorPresenceTest {
    @Test
    fun `ksp provider and duplicate validator are available`() {
        val provider = Class.forName("io.github.mysoundpro.registry.SourceRegistryProcessorProvider")
        val validator = Class.forName("io.github.mysoundpro.registry.MetadataValidator")

        assertThat(provider.interfaces.map { it.name })
            .contains("com.google.devtools.ksp.processing.SymbolProcessorProvider")
        assertThat(validator.methods.map { it.name }).contains("validate")
    }
}
