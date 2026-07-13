package io.github.mysoundpro.api

/** 发生失败时正在执行的领域操作。 */
enum class SourceOperation {
    SEARCH,
    DETAIL,
    CHAPTERS,
    PLAY,
}

/** 可统计且不依赖具体 HTTP 实现的失败类别。 */
enum class SourceErrorKind {
    NETWORK,
    TIMEOUT,
    HTTP_STATUS,
    CHARSET,
    PARSE,
    MEDIA_NOT_FOUND,
    POLICY_VIOLATION,
}

/**
 * Parser 和公共基础设施抛出的结构化异常。
 *
 * [location] 用于记录 CSS selector、JSON field 或解析阶段，避免只留下笼统的
 * “解析失败”。
 */
class SourceException(
    val sourceId: String,
    val operation: SourceOperation,
    val url: String?,
    val kind: SourceErrorKind,
    val location: String? = null,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
