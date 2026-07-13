# MyTingShu 兼容矩阵

核对日期：2026-07-13。

| 检查项 | 结果 | 证据/限制 |
| --- | --- | --- |
| 公开 CustomSources `SourceEntry` 三个静态入口 | 通过 | `getDesc()`、`getCategory()`、`getSources()` 反射测试 |
| 公开 `TingShu` 核心签名 | 通过 | clean-room stub 编译及 Adapter 行为测试 |
| `AudioUrlCustomExtractor.setUp` | 通过 | 多 Source 共享路由的离线播放测试 |
| 宿主类未打入插件 | 通过 | JVM JAR 内容检查拒绝 `com/github/eprendre/tingshu/**` |
| JAR 文件名到入口类映射 | 通过 | `my_sound_pro.jar` 被宿主映射为 `com.github.eprendre.my_sound_pro.SourceEntry` |
| 协程运行时隔离 | 通过 | 协程重定位到 `io.github.mysoundpro.shadow.coroutines`；产物拒绝未重定位的协程 class |
| D8 / Android min API 21 | 通过 | 产物包含 `classes.dex` |
| MyTingShu 2.6.0 宿主加载 | 通过 | Android 10 / API 29 / x86 模拟器冷启动，插件显示 1 个 fixture source |
| 搜索、详情、章节 | 通过 | 搜索 `fixture` 返回 1 本书；详情字段完整；章节列表显示“第 1 集” |
| 播放提取回调 | 通过 | 点击章节后进入 ExoPlayer；保留测试域名按预期报 `UnknownHostException`，无插件 ABI 异常 |
| MP3/AAC/M3U8 实际媒体解码 | Stage 2 | Stage 1 fixture 不提供真实媒体字节，不能据此宣称三种格式均已活体验证 |

## 验证环境

- APP：`com.github.eprendre.tingshu`，`versionName=2.6.0`，`versionCode=2600`，`minSdk=21`，`targetSdk=33`。
- Android：10（API 29），ABI：x86，AVD：`MySoundPro_API29_x86`。
- APK SHA-256：`A21F52F426AC2CF8CCF96FC1FA6C12587DA540B92F343A739A05BF86A638096A`。
- APK MD5：`217746D67FEDBEC7B831A608ABE8207C`，与两个公开下载页公布值一致。
- APK 签名证书 SHA-256：`353A656EBA4EF215B9CD9730913BC01C4DB59E1D118F651EF789DD5173DBA2FD`。
- 截图证据：`outputs/stage1-host-validation/`。

验证时发现并修复了两个真实 ABI 问题：入口包名必须由 JAR 基名派生；宿主父优先类加载会遮蔽插件的 `kotlinx.coroutines`。这两项修复均只位于 `mysound-myting-host` 和构建边界，没有污染 Parser 或领域层。
