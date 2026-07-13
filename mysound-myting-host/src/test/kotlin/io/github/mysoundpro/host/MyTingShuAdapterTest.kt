package io.github.mysoundpro.host

import com.github.eprendre.my_sound_pro.SourceEntry
import com.github.eprendre.tingshu.sources.AudioUrlCustomExtractor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MyTingShuAdapterTest {
    @Test
    fun `maps the generated source into MyTingShu books and episodes`() {
        val source = SourceEntry.getSources().single()

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
        val source = SourceEntry.getSources().single()
        val episode = source.getBookDetailInfo("https://fixture.example.test/book/1").playList.single()

        val mediaUrl = AudioUrlCustomExtractor.resolveForTest(episode.url)

        assertThat(mediaUrl).isEqualTo("https://fixture.example.test/audio/1.mp3")
    }
}
