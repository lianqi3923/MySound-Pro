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

/**
 * 长生命周期组件持有的可切换日志门面。配置重载只替换 delegate，已创建的
 * HttpClient 不必重建；默认 delegate 为 NoOp，保证 Release 静默。
 */
object RuntimeLogger : Logger {
    @Volatile
    private var delegate: Logger = NoOpLogger

    override fun debug(event: LogEvent) = delegate.debug(event)

    @Synchronized
    fun configure(enabled: Boolean, sink: (String) -> Unit = System.out::println) {
        delegate = if (enabled) DebugLogger(true, sink) else NoOpLogger
    }
}
