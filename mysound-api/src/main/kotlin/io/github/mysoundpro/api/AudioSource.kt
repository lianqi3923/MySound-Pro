package io.github.mysoundpro.api

/**
 * 匿名公开音频站点的统一领域契约。
 *
 * 宿主类型不得出现在此接口中；MyTingShu 的同步 API 由 host 模块桥接。
 */
interface AudioSource {
    /** 长期稳定且全项目唯一的来源标识。 */
    val sourceId: String

    /** 显示给用户的来源名称。 */
    val name: String

    /** 规范化的公开站点根地址。 */
    val host: String

    /** 搜索当前来源。 */
    suspend fun search(keyword: String): List<Book>

    /** 获取并补全一本书的详情。 */
    suspend fun detail(url: String): Book

    /** 获取稳定排序的章节列表。 */
    suspend fun chapters(url: String): List<Chapter>

    /** 把公开章节页解析成最终可播放信息。 */
    suspend fun play(url: String): PlayInfo
}
