package io.github.mysoundpro.core.search

import io.github.mysoundpro.api.AudioSource
import io.github.mysoundpro.api.Book
import io.github.mysoundpro.core.log.Logger
import io.github.mysoundpro.core.log.LogEvent
import io.github.mysoundpro.core.log.NoOpLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout

data class SearchFailure(
    val sourceId: String,
    val reason: String,
)

data class SearchReport(
    val books: List<Book>,
    val failures: List<SearchFailure>,
)

class SearchOrchestrator(
    private val timeoutMs: Long = 5_000L,
    private val logger: Logger = NoOpLogger,
) {
    suspend fun search(keyword: String, sources: List<AudioSource>): SearchReport {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isEmpty()) return SearchReport(emptyList(), emptyList())

        val outcomes = supervisorScope {
            sources.map { source ->
                async {
                    try {
                        Outcome.Success(withTimeout(timeoutMs) { source.search(normalizedKeyword) })
                    } catch (timeout: TimeoutCancellationException) {
                        Outcome.Failure(SearchFailure(source.sourceId, "timeout"))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        Outcome.Failure(SearchFailure(source.sourceId, failure.message ?: failure.javaClass.simpleName))
                    }
                }
            }.awaitAll()
        }

        val books = outcomes.filterIsInstance<Outcome.Success>().flatMap { it.books }
        val failures = outcomes.filterIsInstance<Outcome.Failure>().map { it.failure }
        failures.forEach {
            logger.debug(LogEvent("source_search_failed", mapOf("sourceId" to it.sourceId, "reason" to it.reason)))
        }
        return SearchReport(books, failures)
    }

    private sealed interface Outcome {
        data class Success(val books: List<Book>) : Outcome
        data class Failure(val failure: SearchFailure) : Outcome
    }
}
