# MyTingShu 兼容矩阵

核对日期：2026-07-13。

| 检查项 | 结果 | 证据/限制 |
| --- | --- | --- |
| 公开 CustomSources `SourceEntry` 三个静态入口 | 通过 | `getDesc()`、`getCategory()`、`getSources()` 反射测试 |
| 公开 `TingShu` 核心签名 | 通过 | clean-room stub 编译及 Adapter 行为测试 |
| `AudioUrlCustomExtractor.setUp` | 通过 | 多 Source 共享路由的离线播放测试 |
| 宿主类未打入插件 | 通过 | JVM JAR 内容检查拒绝 `com/github/eprendre/tingshu/**` |
| D8 / Android min API 21 | 通过 | 产物包含 `classes.dex` |
| 最新版 APP 真机加载 | 待验证 | 当前工作区未提供 APK、Android 设备或模拟器 |
| 最新版 APP 搜索/详情/章节/播放 | 待验证 | 必须在真实宿主中完成后才能声明完整兼容 |

公开示例仓库本身声明已停止维护，因此公开签名通过不能替代真机验收。若最新版 APP 的私有运行时 ABI 与公开模板不同，应仅修改 `mysound-myting-host`，不得污染 Parser 或领域层。
