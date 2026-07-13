package io.github.mysoundpro.core.log

data class LogEvent(
    val message: String,
    val fields: Map<String, Any?> = emptyMap(),
)

fun interface Logger {
    fun debug(event: LogEvent)
}

class DebugLogger(
    private val enabled: Boolean,
    private val sink: (String) -> Unit,
) : Logger {
    override fun debug(event: LogEvent) {
        if (!enabled) return
        val fields = event.fields
            .toSortedMap()
            .entries
            .joinToString(separator = " ") { (key, value) ->
                val rendered = if (key.isSensitive()) "<redacted>" else value.toString()
                "$key=$rendered"
            }
        sink(if (fields.isEmpty()) event.message else "${event.message} $fields")
    }

    private fun String.isSensitive(): Boolean {
        val normalized = lowercase()
        return listOf("authorization", "cookie", "token", "password", "secret")
            .any(normalized::contains)
    }
}

object NoOpLogger : Logger {
    override fun debug(event: LogEvent) = Unit
}
