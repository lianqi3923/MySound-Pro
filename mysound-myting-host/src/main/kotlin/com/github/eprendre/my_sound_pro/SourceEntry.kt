package com.github.eprendre.my_sound_pro

import com.github.eprendre.tingshu.sources.TingShu
import io.github.mysoundpro.generated.GeneratedSourceRegistry
import io.github.mysoundpro.host.MyTingShuAdapter
import io.github.mysoundpro.host.MyTingShuRuntime
import io.github.mysoundpro.host.SourceConfigRuntime

/** MyTingShu 通过反射调用的唯一插件入口。 */
object SourceEntry {
    @JvmStatic
    fun getDesc(): String = "MySound-Pro：仅聚合无需登录即可访问的公开网页音频源"

    @JvmStatic
    fun getCategory(): String = "听书"

    /**
     * 来源列表由 KSP 生成；新增带注解的 object Parser 后无需修改这里。
     */
    @JvmStatic
    fun getSources(): List<TingShu> {
        val sources = SourceConfigRuntime.enabled(GeneratedSourceRegistry.all())
        MyTingShuRuntime.install(sources)
        return sources.map(::MyTingShuAdapter)
    }
}
