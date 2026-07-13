package io.github.mysoundpro.api

/**
 * KSP 注册器读取的静态来源元数据。
 *
 * 这些值必须是编译期常量，因此能在打包前发现重复来源，而无需 Android 反射扫包。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AudioSourceMetadata(
    val id: String,
    val name: String,
    val host: String,
)
