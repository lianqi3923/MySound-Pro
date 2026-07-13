# MyTingShu 兼容矩阵

核对日期：2026-07-14。

| 检查项 | 结果 | 证据/限制 |
| --- | --- | --- |
| 公开 CustomSources `SourceEntry` 三个静态入口 | 通过 | `getDesc()`、`getCategory()`、`getSources()` 反射测试 |
| 公开 `TingShu` 核心签名 | 通过 | clean-room stub 编译及 Adapter 行为测试 |
| `AudioUrlCustomExtractor.setUp` | 通过 | 多 Source 共享路由的离线播放测试 |
| 宿主类未打入插件 | 通过 | JVM JAR 内容检查拒绝 `com/github/eprendre/tingshu/**` |
| JAR 文件名到入口类映射 | 通过 | `my_sound_pro.jar` 被宿主映射为 `com.github.eprendre.my_sound_pro.SourceEntry` |
| 协程运行时隔离 | 通过 | 协程重定位到 `io.github.mysoundpro.shadow.coroutines`；产物拒绝未重定位的协程 class |
| D8 / Android min API 21 | 通过 | 产物包含 `classes.dex` |
| MyTingShu 2.6.0 宿主加载 | 通过 | 1.0.0 最终 DEX JAR 在 Android 10 / API 29 / x86 模拟器加载为 2 个来源 |
| 搜索、详情、章节 | 通过 | 宿主内搜索 `Alice` 返回 Gutenberg 结果，详情进入播放器并显示 12 章 |
| 播放提取回调 | 通过 | 宿主媒体会话进入 `PlaybackState=3`，媒体为 `19573-01.mp3`，异常日志为空 |
| MP3/AAC/M3U8 地址解析 | 通过 | 直接地址、DOM、JSON、script、iframe 的 MP3/AAC/M4A/M3U8 确定性测试通过 |
| 动态配置 | 通过 | 缺失配置显示 2 个来源；外部配置禁用 LibriVox 后重启显示 1 个来源；非法 JSON 回退由单元测试覆盖 |
| 发布订阅 | 通过 | `update.json` 入口包、版本码和下载 URL 由发布任务生成并校验 |

## 验证环境

- APP：`com.github.eprendre.tingshu`，`versionName=2.6.0`，`versionCode=2600`，`minSdk=21`，`targetSdk=33`。
- Android：10（API 29），ABI：x86，AVD：`MySoundPro_API29_x86`。
- APK SHA-256：`A21F52F426AC2CF8CCF96FC1FA6C12587DA540B92F343A739A05BF86A638096A`。
- APK MD5：`217746D67FEDBEC7B831A608ABE8207C`，与两个公开下载页公布值一致。
- APK 签名证书 SHA-256：`353A656EBA4EF215B9CD9730913BC01C4DB59E1D118F651EF789DD5173DBA2FD`。
- Stage 3 宿主证据：`work/android-avd/stage3-*.xml`（本地验证文件，不进入发布包）。

验证时发现并修复了两个真实 ABI 问题：入口包名必须由 JAR 基名派生；宿主父优先类加载会遮蔽插件的 `kotlinx.coroutines`。这两项修复均只位于 `mysound-myting-host` 和构建边界，没有污染 Parser 或领域层。
