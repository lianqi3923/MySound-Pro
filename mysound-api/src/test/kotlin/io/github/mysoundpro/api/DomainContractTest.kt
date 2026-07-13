package io.github.mysoundpro.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DomainContractTest {
    @Test
    fun `audio source exposes stable suspending operations`() {
        val type = Class.forName("io.github.mysoundpro.api.AudioSource")

        assertThat(type.isInterface).isTrue()
        assertThat(type.methods.map { it.name })
            .contains("getSourceId", "getName", "getHost", "search", "detail", "chapters", "play")

        listOf("search", "detail", "chapters", "play").forEach { methodName ->
            val method = type.methods.single { it.name == methodName }
            assertThat(method.parameterTypes.last().name)
                .describedAs("$methodName must compile as a Kotlin suspend function")
                .isEqualTo("kotlin.coroutines.Continuation")
        }
    }

    @Test
    fun `domain models expose all required metadata`() {
        val bookProperties = gettersOf("io.github.mysoundpro.api.Book")
        val chapterProperties = gettersOf("io.github.mysoundpro.api.Chapter")
        val playProperties = gettersOf("io.github.mysoundpro.api.PlayInfo")

        assertThat(bookProperties).contains(
            "getTitle",
            "getAuthor",
            "getNarrator",
            "getCoverUrl",
            "getCategory",
            "getDescription",
            "getSourceId",
            "getSourceName",
            "getDetailUrl",
        )
        assertThat(chapterProperties).contains("getTitle", "getUrl", "getIndex", "getDurationMs")
        assertThat(playProperties).contains("getUrl", "getFormat", "getHeaders", "getContentType", "getExpiresAtEpochMs")
    }

    @Test
    fun `media format contains supported public formats`() {
        val enumType = Class.forName("io.github.mysoundpro.api.MediaFormat")
        val names = enumType.enumConstants.map { (it as Enum<*>).name }

        assertThat(names).containsExactly("MP3", "M3U8", "AAC", "UNKNOWN")
    }

    @Test
    fun `source failures preserve operation and parse location`() {
        val exceptionType = Class.forName("io.github.mysoundpro.api.SourceException")
        val errorKindType = Class.forName("io.github.mysoundpro.api.SourceErrorKind")
        val operationType = Class.forName("io.github.mysoundpro.api.SourceOperation")

        assertThat(RuntimeException::class.java.isAssignableFrom(exceptionType)).isTrue()
        assertThat(exceptionType.methods.map { it.name }).contains(
            "getSourceId",
            "getOperation",
            "getUrl",
            "getKind",
            "getLocation",
        )
        assertThat(errorKindType.enumConstants.map { (it as Enum<*>).name }).containsExactly(
            "NETWORK",
            "TIMEOUT",
            "HTTP_STATUS",
            "CHARSET",
            "PARSE",
            "MEDIA_NOT_FOUND",
            "POLICY_VIOLATION",
        )
        assertThat(operationType.enumConstants.map { (it as Enum<*>).name }).containsExactly(
            "SEARCH",
            "DETAIL",
            "CHAPTERS",
            "PLAY",
        )
    }

    @Test
    fun `source metadata annotation is retained for registry generation`() {
        val annotation = Class.forName("io.github.mysoundpro.api.AudioSourceMetadata")

        assertThat(annotation.isAnnotation).isTrue()
        assertThat(annotation.declaredMethods.map { it.name }).containsExactlyInAnyOrder("id", "name", "host")
    }

    private fun gettersOf(className: String): Set<String> =
        Class.forName(className).methods.mapTo(mutableSetOf()) { it.name }
}
