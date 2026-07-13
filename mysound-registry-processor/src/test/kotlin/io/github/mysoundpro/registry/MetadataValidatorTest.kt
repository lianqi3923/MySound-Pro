package io.github.mysoundpro.registry

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class MetadataValidatorTest {
    @Test
    fun `accepts unique source metadata`() {
        assertThatCode {
            MetadataValidator.validate(listOf(metadata("a", "https://a.test"), metadata("b", "https://b.test")))
        }.doesNotThrowAnyException()
    }

    @Test
    fun `rejects duplicate source ids`() {
        assertThatThrownBy {
            MetadataValidator.validate(listOf(metadata("same", "https://a.test"), metadata("same", "https://b.test")))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("duplicate source id")
    }

    @Test
    fun `rejects duplicate hosts`() {
        assertThatThrownBy {
            MetadataValidator.validate(listOf(metadata("a", "https://same.test"), metadata("b", "https://same.test")))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("duplicate source host")
    }

    private fun metadata(id: String, host: String) = SourceMetadata(
        qualifiedName = "example.$id",
        id = id,
        name = id,
        host = host,
        isObject = true,
    )
}
