package io.github.mysoundpro.core.http

data class HttpRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val userAgent: String = DEFAULT_USER_AGENT,
    val referer: String? = null,
) {
    companion object {
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 MySound-Pro/0.1"
    }
}

data class HttpResponse(
    val code: Int,
    val finalUrl: String,
    val headers: Map<String, List<String>>,
    val body: ByteArray,
) {
    fun text(fallbackCharset: String = "UTF-8"): String =
        CharsetDecoder.decode(
            body,
            headers.entries.firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value?.firstOrNull(),
            fallbackCharset,
        )
}

fun interface HttpClient {
    suspend fun execute(request: HttpRequest): HttpResponse
}
