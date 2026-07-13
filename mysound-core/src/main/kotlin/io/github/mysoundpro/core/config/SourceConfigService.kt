package io.github.mysoundpro.core.config

import io.github.mysoundpro.core.log.Logger
import io.github.mysoundpro.core.log.LogEvent
import io.github.mysoundpro.core.log.NoOpLogger
import kotlinx.serialization.json.Json
import java.io.File

class SourceConfigService(
    private val defaultJson: String,
    private val externalFile: File? = null,
    private val logger: Logger = NoOpLogger,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val bundledDefault = json.decodeFromString(SourceConfig.serializer(), defaultJson)

    @Volatile
    private var lastGood: SourceConfig = bundledDefault

    @Synchronized
    fun load(): SourceConfig {
        val file = externalFile ?: return lastGood
        if (!file.isFile) return lastGood

        return try {
            json.decodeFromString(SourceConfig.serializer(), file.readText()).also { lastGood = it }
        } catch (failure: Exception) {
            logger.debug(
                LogEvent(
                    message = "config_load_failed",
                    fields = mapOf("path" to file.absolutePath, "reason" to failure.message),
                ),
            )
            lastGood
        }
    }
}
