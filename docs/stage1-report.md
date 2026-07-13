# Stage 1 实施报告

日期：2026-07-13

## 结论

Stage 1 的可自动化部分已经实现：多模块工程、统一契约、HTTP/缓存/配置/日志基础、KSP 自动注册、MyTingShu 适配、离线完整链路、D8 产物检查和 CI。没有实现任何真实资源站 Parser，符合阶段边界。

最新版 MyTingShu APP 的真机加载与播放仍是唯一外部验收项；在没有 APK 和 Android 运行环境的情况下不能诚实地标记为“已验证兼容最新版”。

## 已验证能力

- Source 失败与五秒超时不会取消其他并发 Source。
- HTTP 支持 retry、redirect、UA、Referer、匿名公开 Cookie、gzip、Brotli 和多字符集。
- TTL 缓存具备过期、LRU、失效和同步单次加载语义。
- Debug 日志记录 URL/耗时/错误上下文，并对 Cookie、Authorization、Token 等字段脱敏；Release 可使用 NoOpLogger。
- 配置支持外部文件覆盖、内置默认和 last-good 回退。
- KSP 自动发现带注解的 Source，拒绝重复 ID/host 和错误声明。
- SourceEntry 不含手工 Source 列表；共享音频提取器按章节 URL 路由到正确 Parser。
- clean-room 宿主桩不进入 JVM/DEX 插件产物。

## 本次验证证据

- 命令：`.\\gradlew.bat clean test :mysound-myting-host:verifyD8PluginJar --no-daemon --console=plain -q`
- 结果：退出码 `0`；18 个测试套件、34 个测试，0 failure、0 error、0 skipped。
- DEX JAR：769,336 bytes，包含单一 `classes.dex`（2,103,192 bytes）。
- SHA-256：`3B93FA35771F6C86F21243DB02D07F55295F3203CF52B1CFA6EF116B3DBCDBDD`。
- D8 在本机没有 Android SDK `android.jar` 时会对 `java.*` 平台类型打印 library warning；它不改变退出码和 DEX 内容检查结果。真机门禁仍用于验证宿主平台类与运行时 ABI。

## 已知阶段边界

- Search 的 Merge、去重、排序和健康度将在 Stage 2 与真实多站点一起完成。
- script/JSON/iframe 媒体解析链和 MP3/AAC/M3U8 活体验证属于 Stage 2。
- 动态配置在 Android Scoped Storage 中的最终路径、原生 `update.json` 订阅、正式 Release 元数据和 nightly 100 本活体测试属于 Stage 3。
- Stage 1 fixture 使用保留测试域名，不发起真实站点请求。

## 真机验收清单

1. 在最新版 MyTingShu 导入 Stage 1 DEX JAR。
2. 确认 SourceEntry 被加载且只显示 fixture source。
3. 搜索 `fixture`，打开详情并看到单章节。
4. 触发播放，确认解析到 fixture MP3 URL；由于 URL 使用保留域名，网络播放失败是预期，关键是宿主回调和路由无 ABI 异常。
5. 收集 APP 版本号、Android 版本、设备 ABI 和日志，更新兼容矩阵。
