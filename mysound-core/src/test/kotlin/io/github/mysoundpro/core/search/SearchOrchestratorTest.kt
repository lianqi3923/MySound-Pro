package io.github.mysoundpro.core.search

import io.github.mysoundpro.api.AudioSource
import io.github.mysoundpro.api.Book
import io.github.mysoundpro.api.Chapter
import io.github.mysoundpro.api.MediaFormat
import io.github.mysoundpro.api.PlayInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SearchOrchestratorTest {
    @Test
    fun `source failure and timeout do not cancel successful sources`() = runTest {
        val successful = FakeSource("ok") { listOf(book("ok", "三体")) }
        val failed = FakeSource("failed") { error("site down") }
        val slow = FakeSource("slow") {
            delay(1_000L)
            listOf(book("slow", "迟到"))
        }
        val orchestrator = SearchOrchestrator(timeoutMs = 100L)

        val report = orchestrator.search("三体", listOf(successful, failed, slow))

        assertThat(report.books.map { it.sourceId }).containsExactly("ok")
        assertThat(report.failures.map { it.sourceId }).containsExactlyInAnyOrder("failed", "slow")
    }

    @Test
    fun `blank keywords return without contacting sources`() = runTest {
        var calls = 0
        val source = FakeSource("unused") {
            calls += 1
            emptyList()
        }

        val report = SearchOrchestrator().search("   ", listOf(source))

        assertThat(report.books).isEmpty()
        assertThat(report.failures).isEmpty()
        assertThat(calls).isZero()
    }

    @Test
    fun `merge removes normalized duplicate books and ranks exact title first`() = runTest {
        val first = FakeSource("first") {
            listOf(book("first", "三体"), book("first", "三体 全集"))
        }
        val second = FakeSource("second") {
            listOf(book("second", " 三 体 "), book("second", "三体广播剧"))
        }

        val report = SearchOrchestrator().search("三体", listOf(first, second))

        assertThat(report.books.map { it.title }).containsExactly("三体", "三体 全集", "三体广播剧")
        assertThat(report.health).containsEntry("first", SourceHealth(successes = 1, failures = 0))
        assertThat(report.health).containsEntry("second", SourceHealth(successes = 1, failures = 0))
    }

    @Test
    fun `health report records isolated failures`() = runTest {
        val failed = FakeSource("failed") { error("broken parser") }

        val report = SearchOrchestrator().search("书", listOf(failed))

        assertThat(report.health).containsEntry("failed", SourceHealth(successes = 0, failures = 1))
    }

    private class FakeSource(
        override val sourceId: String,
        private val result: suspend () -> List<Book>,
    ) : AudioSource {
        override val name: String = sourceId
        override val host: String = "https://$sourceId.example.test"
        override suspend fun search(keyword: String): List<Book> = result()
        override suspend fun detail(url: String): Book = error("unused")
        override suspend fun chapters(url: String): List<Chapter> = error("unused")
        override suspend fun play(url: String): PlayInfo = PlayInfo(url, MediaFormat.MP3)
    }

    private fun book(sourceId: String, title: String) = Book(
        title = title,
        sourceId = sourceId,
        sourceName = sourceId,
        detailUrl = "https://$sourceId.example.test/book",
    )
}
