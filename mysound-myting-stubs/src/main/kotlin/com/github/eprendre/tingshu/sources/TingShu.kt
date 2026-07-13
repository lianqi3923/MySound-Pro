package com.github.eprendre.tingshu.sources

import com.github.eprendre.tingshu.utils.Book
import com.github.eprendre.tingshu.utils.BookDetail
import com.github.eprendre.tingshu.utils.Category
import com.github.eprendre.tingshu.utils.CategoryMenu

/** Clean-room 编译桩；运行时由 MyTingShu APP 提供真实类型。 */
abstract class TingShu {
    abstract fun getSourceId(): String
    abstract fun getUrl(): String
    abstract fun getName(): String
    open fun getDesc(): String = ""
    abstract fun search(keywords: String, page: Int): Pair<List<Book>, Int>
    abstract fun getAudioUrlExtractor(): AudioUrlExtractor
    abstract fun getCategoryMenus(): List<CategoryMenu>
    abstract fun getCategoryList(url: String): Category
    abstract fun getBookDetailInfo(
        bookUrl: String,
        loadEpisodes: Boolean = true,
        loadFullPages: Boolean = true,
    ): BookDetail
    open fun isDiscoverable(): Boolean = true
    open fun isWebViewNotRequired(): Boolean = false
}

interface AudioUrlExtractor {
    fun extract(url: String, autoPlay: Boolean, isCache: Boolean, isDebug: Boolean = false)
}

interface AudioUrlExtraHeaders {
    fun headers(audioUrl: String): Map<String, String>
}

/**
 * 测试实现会保存解析回调；真实 APP 中同名对象负责把结果交给播放器。
 */
object AudioUrlCustomExtractor : AudioUrlExtractor {
    private var parser: ((String) -> String)? = null

    fun setUp(parse: (String) -> String) {
        parser = parse
    }

    fun resolveForTest(url: String): String = requireNotNull(parser) { "extractor is not installed" }(url)

    override fun extract(url: String, autoPlay: Boolean, isCache: Boolean, isDebug: Boolean) = Unit
}
