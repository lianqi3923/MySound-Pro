# MySound-Pro

MySound-Pro 是面向《我的听书（MyTingShu）》的开源听书源插件。项目只接入无需登录、无需会员、无需私有 Cookie/Token/签名且可由普通浏览器公开访问的网页。

当前状态：**Stage 1 基础骨架**。本阶段仅包含离线 fixture source，用于验证架构、自动注册和宿主适配；尚未上线任何真实资源站。

## 技术基线

- Kotlin 1.9.24、Gradle 8.10.2、JDK 17
- JVM target 1.8，D8 min API 21
- OkHttp、Jsoup、Coroutines、JUnit 5、KSP
- MIT License

## 模块

| 模块 | 职责 |
| --- | --- |
| `mysound-api` | `AudioSource`、Book、Chapter、PlayInfo 与结构化异常 |
| `mysound-core` | HTTP、Brotli/gzip、缓存、配置、日志、并发搜索基础 |
| `mysound-registry-processor` | KSP 扫描 Source 元数据并生成注册表 |
| `mysound-sources` | 每站一个独立 Parser；Stage 1 仅有离线 fixture |
| `mysound-myting-stubs` | clean-room 宿主签名，仅供编译测试，不进入产物 |
| `mysound-myting-host` | MyTingShu Adapter、SourceEntry 与 D8 打包 |
| `mysound-testkit` | 跨模块测试夹具扩展点 |

## 构建

```bash
./gradlew clean test :mysound-myting-host:verifyD8PluginJar
```

Windows：

```powershell
.\gradlew.bat clean test :mysound-myting-host:verifyD8PluginJar
```

DEX JAR 输出到 `mysound-myting-host/build/release/my_sound_pro.jar`。

## 新增站点

新增站点只需在 `mysound-sources` 增加一个带 `@AudioSourceMetadata` 的 Kotlin `object` 并实现 `AudioSource`。KSP 会自动生成注册表；不允许修改 `SourceEntry` 或维护手工列表。重复 `id`、重复标准化 host、非 object 或未实现接口会在编译期失败。

Parser 应只包含站点选择器和字段映射，共享请求、缓存、日志及媒体解析逻辑必须放在公共模块。单个 Parser 上限为 300 行。

## 合规边界

项目不接入喜马拉雅、番茄、懒人、QQ 阅读、微信等登录或会员平台，也不实现验证码规避、私有接口签名、付费内容解锁或反爬绕过。站点即使技术上可解析，只要不满足公开访问准入条件，也不会合入。

## 阶段门禁

- Stage 1：架构、HTTP、缓存、KSP、宿主适配、D8 与离线链路。
- Stage 2：审计并实现首批 2～3 个合规公开站点，增加媒体解析链与 100 本活体测试。
- Stage 3：正式发布、原生订阅更新、动态配置闭环和长期维护设施。

详见 `docs/stage1-report.md` 和初始架构设计。Stage 1 确认前不会开始真实站点适配。
