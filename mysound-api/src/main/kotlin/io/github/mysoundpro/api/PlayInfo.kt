package io.github.mysoundpro.api

/** 最终交给宿主播放器的公开媒体请求。 */
data class PlayInfo(
    val url: String,
    val format: MediaFormat,
    val headers: Map<String, String> = emptyMap(),
    val contentType: String? = null,
    val expiresAtEpochMs: Long? = null,
)
