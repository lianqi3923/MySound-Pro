package io.github.mysoundpro.api

/** 可由所属 [AudioSource] 继续解析的章节。 */
data class Chapter(
    val title: String,
    val url: String,
    val index: Int,
    val durationMs: Long? = null,
)
