# MySound-Pro

MySound-Pro 是《我的听书（MyTingShu）》的开源音源插件，只接入普通浏览器无需登录即可访问的公开音频。项目不实现会员解锁、验证码绕过、私有 Cookie、Token、签名算法或 DRM 规避。

当前版本：**1.1.0**。已验证宿主：MyTingShu 2.6.0；构建使用 Kotlin、Gradle 与 JDK 17，插件最低 Android API 21。

## 已启用来源

| 来源 | 内容与接口 | 状态 |
|---|---|---|
| Project Gutenberg 音频 | Gutendex 公开目录、Gutenberg 公开 MP3 | 默认启用 |
| LibriVox | 官方 released API、公版志愿者录音 | 默认启用 |
| 听书网 | 公开 HTML 搜索、详情、章节及播放页 | 默认启用 |
| 博看有声 | 公开目录与章节 API、AAC/M4A 音频 | 默认启用 |
| 云图有声 | 公开目录与章节 API、MP3 音频 | 默认启用 |

69 听书因搜索验证码、Internet Archive 因本次准入环境持续连接超时而隔离，未进入自动注册。完整记录见 [站点准入](docs/site-admission.md)。

## 安装与更新

1. 从 Release 获取 `my_sound_pro.jar`。
2. 按 MyTingShu 的自定义来源功能导入 JAR；文件名必须保持 `my_sound_pro.jar`。
3. 原生更新订阅使用 Release 中的 `update.json`。订阅字段 `entry_package` 固定为 JAR 基名 `my_sound_pro`；宿主由此查找 `com.github.eprendre.my_sound_pro.SourceEntry`。

项目不在插件启动时自行联网弹窗；更新提示交给 MyTingShu 原生订阅机制，避免阻塞宿主或伪造 Android UI 能力。

## 动态配置

默认路径：

```text
/sdcard/Android/data/com.github.eprendre.tingshu/files/my_sound_pro/config.json
```

复制 [config.example.json](config/config.example.json) 后修改 `enabled`。MyTingShu 下次读取来源列表时重新加载文件，无需重新编译。非法 JSON 会保留上一次有效配置；未配置的新来源默认启用。

开发/桌面环境可用 JVM 属性 `-Dmysound.config=/absolute/path/config.json` 或环境变量 `MYSOUND_CONFIG` 覆盖路径。`debug=false` 时统一日志器完全静默；开启后输出请求 URL、耗时和结构化失败位置，并自动脱敏凭据字段。

## 构建与测试

`gradle.properties` 中的 `projectUrl` 指向项目公开仓库；GitHub Actions 也会使用当前仓库地址覆盖该值，保证 Fork 后发布链接正确。

```bash
./gradlew clean test :mysound-myting-host:verifyReleaseBundle
```

Windows：

```powershell
.\gradlew.bat clean test :mysound-myting-host:verifyReleaseBundle
```

发布目录：`mysound-myting-host/build/release/`。线上测试默认关闭；手工运行固定种子的 100 本低频测试：

```bash
./gradlew :mysound-sources:test \
  --tests "io.github.mysoundpro.sources.LiveSourceReliabilityTest.random*" \
  -Pmysound.live=true -Pmysound.live.sampleSize=100
```

普通 PR 只运行 fixture，不访问第三方站点。最新一次 100 本详情抽样为 100/100，详见 [Stage 2 报告](docs/stage2-report.md)；发布与宿主回归见 [Stage 3 报告](docs/stage3-report.md)。

## 架构

| 模块 | 职责 |
|---|---|
| `mysound-api` | `AudioSource`、Book、Chapter、PlayInfo 和结构化异常 |
| `mysound-core` | HTTP、重试、匿名 Cookie、字符集、缓存、日志、并发搜索、媒体解析链 |
| `mysound-registry-processor` | KSP 编译期自动注册和重复元数据检查 |
| `mysound-sources` | 每站一个独立 Parser，单文件不超过 300 行 |
| `mysound-myting-stubs` | clean-room 宿主签名，只用于编译测试 |
| `mysound-myting-host` | MyTingShu Adapter、动态配置、D8 与发布包 |
| `mysound-testkit` | 跨模块测试扩展点 |

新增站点只需新增 Parser 和 fixture，不修改 `SourceEntry` 或手工列表。操作步骤见 [新增来源指南](docs/adding-source.md)。

## 合规与维护

- 不托管或镜像音频，不批量下载媒体。
- 站点出现登录、验证码、Token、签名或会员要求时立即隔离，不加入绕过逻辑。
- Nightly 低频检查失败只告警，不自动修改 Parser。
- 内容权利问题请提交 issue；确认后可通过配置和后续版本快速停用来源。

项目采用 [MIT License](LICENSE)。变更记录见 [CHANGELOG](CHANGELOG.md)。
