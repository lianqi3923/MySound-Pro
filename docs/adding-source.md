# 新增来源指南

## 准入条件

来源必须允许普通浏览器直接完成搜索、详情、章节和播放，不得要求登录、会员、验证码、私有 Cookie、Token、请求签名或 DRM 绕过。先记录站点条款、robots、访问稳定性和媒体类型；不满足条件时进入隔离，不写规避代码。

## 文件与注册

1. 在 `mysound-sources/src/main/kotlin/io/github/mysoundpro/sources/` 新增一个站点文件。
2. 实现可注入 `HttpClient` 的 `CachedAudioSource` 子类。
3. 用带 `@AudioSourceMetadata` 的 Kotlin `object` 委托该实现。
4. 不修改 `SourceEntry`。KSP 会自动生成注册表，并在重复 id、host 或错误类型时使构建失败。

```kotlin
@AudioSourceMetadata(id = "example", name = "Example", host = "https://example.org")
object Example : AudioSource by ExampleSource()

internal class ExampleSource(
    private val httpClient: HttpClient = HttpClientFactory.create(),
) : CachedAudioSource() {
    // 只保留站点选择器和字段映射。
}
```

## 解析规则

- HTML 必须用 Jsoup，不用正则解析 DOM。
- `play()` 优先交给 `MediaResolverChain`，不要复制 script/JSON/iframe 逻辑。
- 公共重试、重定向、UA、Referer、Cookie、Brotli、字符集、缓存和日志不得放进站点 Parser。
- 失败抛出带 `operation`、`kind` 和 `location` 的 `SourceException`。
- Parser 单文件硬上限 300 行。

## 测试与提交

每站至少提交：搜索 fixture、详情/章节 fixture、播放 fixture、错误页 fixture，以及 search/detail/chapters/play 契约测试。普通测试不得访问网络。

准入后运行线上 smoke 和固定 seed 的低频抽样；把成功率、失败类型和日期写入 `docs/site-admission.md`。低于阈值或出现鉴权时移除注解并默认禁用，而不是让其它来源受影响。
