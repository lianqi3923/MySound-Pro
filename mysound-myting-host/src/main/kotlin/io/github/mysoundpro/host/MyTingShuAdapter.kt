package io.github.mysoundpro.host

import com.github.eprendre.tingshu.sources.AudioUrlCustomExtractor
import com.github.eprendre.tingshu.sources.AudioUrlExtraHeaders
import com.github.eprendre.tingshu.sources.AudioUrlExtractor
import com.github.eprendre.tingshu.sources.TingShu
import com.github.eprendre.tingshu.utils.BookDetail
import com.github.eprendre.tingshu.utils.Category
import com.github.eprendre.tingshu.utils.CategoryMenu
import com.github.eprendre.tingshu.utils.Episode
import io.github.mysoundpro.api.AudioSource
import kotlinx.coroutines.runBlocking
import com.github.eprendre.tingshu.utils.Book as HostBook
import io.github.mysoundpro.api.Book as DomainBook

/** 将稳定的项目内契约映射到 MyTingShu 当前公开插件 API。 */
class MyTingShuAdapter(private val delegate: AudioSource) : TingShu(), AudioUrlExtraHeaders {
    override fun getSourceId(): String = delegate.sourceId

    override fun getUrl(): String = delegate.host

    override fun getName(): String = delegate.name

    override fun getDesc(): String = "MySound-Pro 公开网页源：${delegate.name}"

    override fun search(keywords: String, page: Int): Pair<List<HostBook>, Int> {
        if (page > 1) return emptyList<HostBook>() to 1
        return runBlocking { delegate.search(keywords) }.map(::toHostBook) to 1
    }

    override fun getAudioUrlExtractor(): AudioUrlExtractor = AudioUrlCustomExtractor

    override fun getCategoryMenus(): List<CategoryMenu> = emptyList()

    override fun getCategoryList(url: String): Category = Category(
        list = emptyList(),
        currentPage = 1,
        totalPage = 1,
        currentUrl = url,
        nextUrl = "",
    )

    override fun getBookDetailInfo(
        bookUrl: String,
        loadEpisodes: Boolean,
        loadFullPages: Boolean,
    ): BookDetail = runBlocking {
        val book = delegate.detail(bookUrl)
        val chapters = if (loadEpisodes) delegate.chapters(bookUrl) else emptyList()
        MyTingShuRuntime.track(delegate, chapters.map { it.url })
        BookDetail(
            playList = chapters.sortedBy { it.index }.map { Episode(it.title, it.url) },
            intro = book.description.orEmpty(),
            artist = book.narrator.orEmpty(),
            author = book.author.orEmpty(),
            episodesCount = chapters.size,
            coverUrl = book.coverUrl.orEmpty(),
        )
    }

    override fun headers(audioUrl: String): Map<String, String> = MyTingShuRuntime.headers(audioUrl)

    override fun isDiscoverable(): Boolean = false

    override fun isWebViewNotRequired(): Boolean = true

    private fun toHostBook(book: DomainBook): HostBook = HostBook(
        coverUrl = book.coverUrl.orEmpty(),
        bookUrl = book.detailUrl,
        title = book.title,
        author = book.author.orEmpty(),
        artist = book.narrator.orEmpty(),
    ).also {
        it.intro = book.description.orEmpty()
        it.sourceId = book.sourceId
    }
}
