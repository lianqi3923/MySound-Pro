# Changelog

本项目遵循语义化版本。

## [1.0.0] - 2026-07-14

### Added

- Project Gutenberg 音频与 LibriVox 两个默认启用的公开公版来源。
- 直接媒体、DOM、JSON、静态 script 与 iframe 媒体解析链，支持 MP3、AAC/M4A、M3U8。
- 多站协程搜索合并、规范化去重、相关性排序和单站健康统计。
- 搜索、详情、章节五分钟缓存及动态 `config.json` 启停与上次有效配置回退。
- MyTingShu 原生 `update.json`、发布清单、SHA-256、CycloneDX SBOM 和第三方许可清单。
- PR CI、nightly 100 本低频活体测试和 tag release 自动发布。

### Changed

- Jsoup、Serialization 和 Coroutines 重定位到插件私有命名空间，避免宿主父优先类加载冲突。
- Stage 1 fixture source 退出生产注册。

### Verified

- 49 项 Stage 2 离线测试全部通过；100 本线上详情抽样成功率 100%。
- D8 发布 JAR 内容、重复依赖、Parser 行数与发布元数据校验通过。

## [0.1.0-stage1] - 2026-07-13

### Added

- Kotlin/Gradle/JDK 17 多模块骨架与 Gradle Wrapper。
- suspending `AudioSource` 统一契约及领域对象。
- OkHttp 请求封装、重试、重定向、匿名 Cookie、gzip/Brotli 和字符集处理。
- 五分钟 TTL 缓存基础、Debug 日志脱敏、JSON 配置回退和五秒并发搜索隔离。
- KSP 自动 Source 注册及重复元数据编译检查。
- clean-room MyTingShu 宿主桩、SourceEntry、Adapter 和共享音频路由。
- 离线 fixture 端到端测试、D8 内容校验和 GitHub Actions CI。

### Fixed

- 按 `my_sound_pro.jar` 基名导出宿主实际查找的 `com.github.eprendre.my_sound_pro.SourceEntry`。
- 将协程运行时重定位到插件私有包，消除 MyTingShu 2.6.0 父优先类加载导致的 ABI 冲突。

### Verified

- 在 Android 10 / API 29 / x86 模拟器中完成 MyTingShu 2.6.0 的加载、启用、搜索、详情、章节与播放提取冒烟。
