# Stage 1 实施报告

日期：2026-07-13

## 结论

Stage 1 已完成：多模块工程、统一契约、HTTP/缓存/配置/日志基础、KSP 自动注册、MyTingShu 适配、离线完整链路、D8 产物检查、CI，以及 MyTingShu 2.6.0 真实宿主冒烟。没有实现任何真实资源站 Parser，符合阶段边界。

## 已验证能力

- Source 失败与五秒超时不会取消其他并发 Source。
- HTTP 支持 retry、redirect、UA、Referer、匿名公开 Cookie、gzip、Brotli 和多字符集。
- TTL 缓存具备过期、LRU、失效和同步单次加载语义。
- Debug 日志记录 URL/耗时/错误上下文，并对 Cookie、Authorization、Token 等字段脱敏；Release 可使用 NoOpLogger。
- 配置支持外部文件覆盖、内置默认和 last-good 回退。
- KSP 自动发现带注解的 Source，拒绝重复 ID/host 和错误声明。
- SourceEntry 不含手工 Source 列表；共享音频提取器按章节 URL 路由到正确 Parser。
- clean-room 宿主桩不进入 JVM/DEX 插件产物。
- `my_sound_pro.jar` 导出宿主实际查找的 `com.github.eprendre.my_sound_pro.SourceEntry`。
- 协程运行时被重定位到插件私有包，避免宿主父优先类加载造成 ABI 冲突。

## 本次验证证据

- 自动化命令：`.\gradlew.bat clean test :mysound-myting-host:verifyD8PluginJar --no-daemon --console=plain`。
- 自动化结果：18 个测试套件、34 个测试，0 failure、0 error、0 skipped；Gradle 退出码 0。
- DEX JAR：771,040 bytes，包含单一 `classes.dex`（2,128,964 bytes）；SHA-256：`7372DF68B386059C9EFD8D135A42810ED15B25BB0B208C96A8500D71C24AF62D`。
- 宿主：MyTingShu 2.6.0（versionCode 2600），Android 10 / API 29 / x86。
- 加载：宿主识别 `my_sound_pro`，显示 1 个 fixture source，可正常启用。
- 搜索：`fixture` 返回 1 本“Fixture 有声书”，作者、主播、简介和来源映射正确。
- 详情/章节：进入播放详情页，章节列表显示“第 1 集（1/1）”。
- 播放：点击章节后 ExoPlayer 发起请求；fixture 使用保留测试域名，因此 `UnknownHostException` 是预期结果；日志中无 `ClassNotFound`、`NoClassDefFound`、`NoSuchMethod`、`VerifyError` 或 `AbstractMethodError` 等插件异常。
- 屏幕证据：`outputs/stage1-host-validation/search.png`、`chapters.png`、`player.png`。

## 已知阶段边界

- Search 的 Merge、去重、排序和健康度将在 Stage 2 与真实多站点一起完成。
- script/JSON/iframe 媒体解析链和 MP3/AAC/M3U8 活体验证属于 Stage 2。
- 动态配置在 Android Scoped Storage 中的最终路径、原生 `update.json` 订阅、正式 Release 元数据和 nightly 100 本活体测试属于 Stage 3。
- Stage 1 fixture 使用保留测试域名，不发起真实站点请求。

## 宿主验证中修复的问题

1. MyTingShu 2.6.0 由 JAR 基名推导入口包。原包名导致 `ClassNotFoundException`，已改为 `com.github.eprendre.my_sound_pro.SourceEntry`，并加入反射和 JAR 内容门禁。
2. 宿主采用父优先类加载，且其协程类经过 R8/加固处理，不能作为稳定插件 ABI。直接使用时先后出现 `NoSuchMethodError` 和 `NoClassDefFoundError`；现通过 Shadow 将协程完整重定位到项目私有命名空间，并加入未重定位 class 拒绝规则。

Stage 1 到此停止。MP3/AAC/M3U8 真实媒体解码、真实站点 Parser 和 100 本随机活体测试仍按计划属于 Stage 2/3。
