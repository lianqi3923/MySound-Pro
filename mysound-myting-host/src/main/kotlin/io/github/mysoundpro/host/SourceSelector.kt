package io.github.mysoundpro.host

import io.github.mysoundpro.api.AudioSource
import io.github.mysoundpro.core.config.SourceConfigService
import io.github.mysoundpro.core.log.RuntimeLogger
import java.io.File

/** 每次宿主读取来源时重新加载外部配置，实现无需重新编译的启停。 */
internal class SourceSelector(
    private val configService: SourceConfigService,
) {
    fun enabled(sources: List<AudioSource>): List<AudioSource> {
        val config = configService.load()
        RuntimeLogger.configure(config.debug)
        return sources.filter { config.sources[it.sourceId]?.enabled != false }
    }
}

/** SourceEntry 使用的宿主配置边界；Android 路径可由属性或环境变量覆盖。 */
internal object SourceConfigRuntime {
    private val selector: SourceSelector by lazy {
        SourceSelector(
            SourceConfigService(
                // D8 产出的插件 JAR 只保证携带 classes.dex，因此默认值必须编译进字节码。
                defaultJson = DEFAULT_JSON,
                externalFile = externalConfigFile(),
                logger = RuntimeLogger,
            ),
        )
    }

    fun enabled(sources: List<AudioSource>): List<AudioSource> = selector.enabled(sources)

    private fun externalConfigFile(): File {
        val override = System.getProperty("mysound.config")
            ?.takeIf(String::isNotBlank)
            ?: System.getenv("MYSOUND_CONFIG")?.takeIf(String::isNotBlank)
        return if (override != null) File(override) else File(DEFAULT_ANDROID_PATH)
    }

    private const val DEFAULT_ANDROID_PATH =
        "/sdcard/Android/data/com.github.eprendre.tingshu/files/my_sound_pro/config.json"

    private const val DEFAULT_JSON =
        """{"schemaVersion":1,"debug":false,"sources":{"gutenberg-audio":{"enabled":true},"librivox":{"enabled":true}}}"""
}
