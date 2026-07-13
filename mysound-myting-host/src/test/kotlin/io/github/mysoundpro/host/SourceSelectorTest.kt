package io.github.mysoundpro.host

import io.github.mysoundpro.api.AudioSource
import io.github.mysoundpro.api.Book
import io.github.mysoundpro.api.Chapter
import io.github.mysoundpro.api.MediaFormat
import io.github.mysoundpro.api.PlayInfo
import io.github.mysoundpro.core.config.SourceConfigService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SourceSelectorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `external file can disable and re-enable a source without recompiling`() {
        val file = tempDir.resolve("config.json").toFile()
        val service = SourceConfigService(
            defaultJson = """{"schemaVersion":1,"sources":{}}""",
            externalFile = file,
        )
        val selector = SourceSelector(service)

        file.writeText("""{"schemaVersion":1,"sources":{"b":{"enabled":false}}}""")
        assertThat(selector.enabled(listOf(source("a"), source("b"))).map { it.sourceId }).containsExactly("a")

        file.writeText("""{"schemaVersion":1,"sources":{"b":{"enabled":true}}}""")
        assertThat(selector.enabled(listOf(source("a"), source("b"))).map { it.sourceId }).containsExactly("a", "b")
    }

    @Test
    fun `invalid update keeps last enabled source snapshot`() {
        val file = tempDir.resolve("config.json").toFile().apply {
            writeText("""{"schemaVersion":1,"sources":{"a":{"enabled":false}}}""")
        }
        val selector = SourceSelector(SourceConfigService("""{"schemaVersion":1}""", file))
        assertThat(selector.enabled(listOf(source("a"), source("b"))).map { it.sourceId }).containsExactly("b")

        file.writeText("{invalid")

        assertThat(selector.enabled(listOf(source("a"), source("b"))).map { it.sourceId }).containsExactly("b")
    }

    private fun source(id: String) = object : AudioSource {
        override val sourceId = id
        override val name = id
        override val host = "https://$id.example.test"
        override suspend fun search(keyword: String): List<Book> = emptyList()
        override suspend fun detail(url: String): Book = error("unused")
        override suspend fun chapters(url: String): List<Chapter> = emptyList()
        override suspend fun play(url: String) = PlayInfo(url, MediaFormat.MP3)
    }
}
