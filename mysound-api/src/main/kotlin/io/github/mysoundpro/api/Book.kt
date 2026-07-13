package io.github.mysoundpro.api

/** 一本公开有声书的领域快照。 */
data class Book(
    val title: String,
    val author: String? = null,
    val narrator: String? = null,
    val coverUrl: String? = null,
    val category: String? = null,
    val description: String? = null,
    val sourceId: String,
    val sourceName: String,
    val detailUrl: String,
)
