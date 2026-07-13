package io.github.mysoundpro.core.config

import kotlinx.serialization.Serializable

@Serializable
data class SourceConfig(
    val schemaVersion: Int = 1,
    val debug: Boolean = false,
    val sources: Map<String, SourceSettings> = emptyMap(),
)

@Serializable
data class SourceSettings(
    val enabled: Boolean = true,
    val userAgent: String? = null,
    val charset: String? = null,
)
