package io.github.mysoundpro.sources

import io.github.mysoundpro.api.AudioSource
import io.github.mysoundpro.api.AudioSourceMetadata
import io.github.mysoundpro.api.Book
import io.github.mysoundpro.api.Chapter
import io.github.mysoundpro.api.MediaFormat
import io.github.mysoundpro.api.PlayInfo

/**
 * Stage 1 的离线契约源，只使用保留域名和静态数据。
 *
 * 它用于验证自动注册与宿主映射，不代表已接入真实资源站。
 */
@AudioSourceMetadata(
    id = "fixture-public",
    name = "MySound-Pro Fixture",
    host = "https://fixture.example.test",
)
object FixtureAudioSource : AudioSource {
    override val sourceId: String = "fixture-public"
    override val name: String = "MySound-Pro Fixture"
    override val host: String = "https://fixture.example.test"

    override suspend fun search(keyword: String): List<Book> = listOf(book())

    override suspend fun detail(url: String): Book = book()

    override suspend fun chapters(url: String): List<Chapter> = listOf(
        Chapter("第 1 集", "$host/chapter/1", index = 0, durationMs = 60_000L),
    )

    override suspend fun play(url: String): PlayInfo = PlayInfo(
        url = "$host/audio/1.mp3",
        format = MediaFormat.MP3,
        contentType = "audio/mpeg",
    )

    private fun book() = Book(
        title = "Fixture 有声书",
        author = "Fixture Author",
        narrator = "Fixture Narrator",
        category = "测试",
        description = "只用于 Stage 1 离线验证。",
        sourceId = sourceId,
        sourceName = name,
        detailUrl = "$host/book/1",
    )
}
