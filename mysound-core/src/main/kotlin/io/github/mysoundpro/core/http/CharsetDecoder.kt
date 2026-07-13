package io.github.mysoundpro.core.http

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object CharsetDecoder {
    private val headerCharset = Regex("charset\\s*=\\s*[\\\"']?([^;\\s\\\"']+)", RegexOption.IGNORE_CASE)
    private val metaCharset = Regex("charset\\s*=\\s*[\\\"']?([A-Za-z0-9._-]+)", RegexOption.IGNORE_CASE)

    fun decode(bytes: ByteArray, contentType: String?, fallbackCharset: String = "UTF-8"): String {
        val charset = charsetFromHeader(contentType)
            ?: charsetFromBom(bytes)
            ?: charsetFromMeta(bytes)
            ?: Charset.forName(fallbackCharset)
        val offset = bomLength(bytes, charset)
        return String(bytes, offset, bytes.size - offset, charset)
    }

    private fun charsetFromHeader(contentType: String?): Charset? =
        contentType?.let(headerCharset::find)?.groupValues?.get(1)?.toCharsetOrNull()

    private fun charsetFromMeta(bytes: ByteArray): Charset? {
        val prefix = bytes.copyOfRange(0, minOf(bytes.size, 4_096)).toString(StandardCharsets.ISO_8859_1)
        return metaCharset.find(prefix)?.groupValues?.get(1)?.toCharsetOrNull()
    }

    private fun charsetFromBom(bytes: ByteArray): Charset? = when {
        bytes.startsWith(0xEF, 0xBB, 0xBF) -> StandardCharsets.UTF_8
        bytes.startsWith(0xFE, 0xFF) -> StandardCharsets.UTF_16BE
        bytes.startsWith(0xFF, 0xFE) -> StandardCharsets.UTF_16LE
        else -> null
    }

    private fun bomLength(bytes: ByteArray, charset: Charset): Int = when {
        charset == StandardCharsets.UTF_8 && bytes.startsWith(0xEF, 0xBB, 0xBF) -> 3
        (charset == StandardCharsets.UTF_16BE || charset == StandardCharsets.UTF_16LE) && bytes.size >= 2 -> 2
        else -> 0
    }

    private fun ByteArray.startsWith(vararg prefix: Int): Boolean =
        size >= prefix.size && prefix.indices.all { this[it].toInt() and 0xFF == prefix[it] }

    private fun String.toCharsetOrNull(): Charset? = runCatching { Charset.forName(this) }.getOrNull()
}
