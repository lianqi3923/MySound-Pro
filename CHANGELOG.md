# Changelog

本项目遵循语义化版本。正式站点和稳定发布将在后续阶段加入。

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
