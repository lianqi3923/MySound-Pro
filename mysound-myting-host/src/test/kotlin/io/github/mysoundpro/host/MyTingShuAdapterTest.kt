package io.github.mysoundpro.host

import com.github.eprendre.tingshu.sources.AudioUrlCustomExtractor
import io.github.mysoundpro.api.AudioSource
import io.github.mysoundpro.api.Book
import io.github.mysoundpro.api.Chapter
import io.github.mysoundpro.api.MediaFormat
import io.github.mysoundpro.api.PlayInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MyTingShuAdapterTest {
    @Test
    fun `maps the generated source into MyTingShu books and episodes`() {
        val source = MyTingShuAdapter(FakeSource)

        val (books, totalPages) = source.search("fixture", 1)
        val book = books.single()
        val detail = source.getBookDetailInfo(book.bookUrl)

        assertThat(source.getSourceId()).isEqualTo("fixture-public")
        assertThat(book.title).isEqualTo("Fixture 有声书")
        assertThat(book.sourceId).isEqualTo("fixture-public")
        assertThat(totalPages).isEqualTo(1)
        assertThat(detail.playList.single().title).isEqualTo("第 1 集")
        assertThat(source.isDiscoverable()).isFalse()
        assertThat(source.isWebViewNotRequired()).isTrue()
    }

    @Test
    fun `routes a chapter through the shared custom audio extractor`() {
        MyTingShuRuntime.install(listOf(FakeSource))
        val source = MyTingShuAdapter(FakeSource)
        val episode = source.getBookDetailInfo("https://fixture.example.test/book/1").playList.single()

        val mediaUrl = AudioUrlCustomExtractor.resolveForTest(episode.url)

        assertThat(mediaUrl).isEqualTo("https://fixture.example.test/audio/1.mp3")
    }

    private object FakeSource : AudioSource {
        override val sourceId = "fixture-public"
        override val name = "Fixture"
        override val host = "https://fixture.example.test"
        override suspend fun search(keyword: String) = listOf(book())
        override suspend fun detail(url: String) = book()
        override suspend fun chapters(url: String) = listOf(Chapter("第 1 集", "$host/chapter/1", 0))
        override suspend fun play(url: String) = PlayInfo("$host/audio/1.mp3", MediaFormat.MP3)
        private fun book() = Book(
            title = "Fixture 有声书",
            sourceId = sourceId,
            sourceName = name,
            detailUrl = "$host/book/1",
        )
    }
}
