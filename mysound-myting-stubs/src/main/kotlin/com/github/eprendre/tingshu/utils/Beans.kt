package com.github.eprendre.tingshu.utils

/**
 * Clean-room 编译桩，只描述 MyTingShu 公开自定义源 API 的二进制签名。
 * 本模块使用 compileOnly 引入，绝不会被打包进插件。
 */
data class Book(
    var coverUrl: String,
    val bookUrl: String,
    var title: String,
    var author: String,
    var artist: String,
) {
    var intro: String = ""
    var sourceId: String? = null
}

data class Episode(val title: String, val url: String)

data class BookDetail(
    val playList: List<Episode>,
    val intro: String? = "",
    val artist: String = "",
    val author: String = "",
    val episodesCount: Int = 0,
    val coverUrl: String = "",
)

data class CategoryTab(val title: String, val url: String)

data class CategoryMenu(val title: String, val tabs: List<CategoryTab>)

data class Category(
    val list: List<Book>,
    val currentPage: Int,
    val totalPage: Int,
    val currentUrl: String,
    val nextUrl: String,
)
