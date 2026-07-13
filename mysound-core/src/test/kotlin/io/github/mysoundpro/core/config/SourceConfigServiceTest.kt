package io.github.mysoundpro.core.config

import io.github.mysoundpro.core.log.LogEvent
import io.github.mysoundpro.core.log.Logger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SourceConfigServiceTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `external configuration overrides bundled defaults`() {
        val file = tempDir.resolve("config.json").toFile().apply {
            writeText("""{"schemaVersion":1,"debug":true,"sources":{"ting56":{"enabled":false}}}""")
        }
        val service = SourceConfigService(
            defaultJson = """{"schemaVersion":1,"debug":false}""",
            externalFile = file,
        )

        val config = service.load()

        assertThat(config.debug).isTrue()
        assertThat(config.sources.getValue("ting56").enabled).isFalse()
    }

    @Test
    fun `invalid external update keeps the last valid snapshot`() {
        val events = mutableListOf<LogEvent>()
        val file = tempDir.resolve("config.json").toFile().apply {
            writeText("""{"schemaVersion":1,"debug":true}""")
        }
        val service = SourceConfigService(
            defaultJson = """{"schemaVersion":1,"debug":false}""",
            externalFile = file,
            logger = Logger(events::add),
        )
        assertThat(service.load().debug).isTrue()

        file.writeText("{invalid")
        val recovered = service.load()

        assertThat(recovered.debug).isTrue()
        assertThat(events).anyMatch { it.message == "config_load_failed" }
    }
}
