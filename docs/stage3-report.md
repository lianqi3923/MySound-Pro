# Stage 3 实施报告

## 发布与维护能力

- 外部 `config.json` 可按 `sourceId` 动态启停来源；默认配置编译进 DEX，不依赖 JAR 资源。
- Debug 日志可运行时启停，Release 默认静默；非法配置保留上一次有效快照。
- 发布任务生成 DEX JAR、插件清单、更新订阅、CycloneDX SBOM、校验和、第三方声明与项目文档。
- 协程、序列化和 Jsoup 运行时重定位，构建门禁拒绝宿主 stub、重复 Kotlin/OkHttp 和未重定位 class。
- GitHub Actions 提供离线 CI、Nightly 100 本低频测试和 SemVer Tag Release。
- Parser 单文件 300 行限制已纳入 Gradle `check`。

## 自动化验证

执行日期：2026-07-14。

- `clean test :mysound-myting-host:verifyReleaseBundle`：53 项测试，0 失败，2 项显式 opt-in 在线测试跳过。
- YAML Lint 与 actionlint 1.7.12：通过。
- 两次独立 `clean` 构建的 `my_sound_pro.jar` SHA-256 完全一致：
  `F7F534767A167E21DFAD99AAA81646BD36BC29BBD4FA1ED2C048D7FF56CA7966`。
- 发布包清单、版本码、入口包和 JAR 校验和一致性：通过。

## MyTingShu 宿主回归

环境：MyTingShu 2.6.0，Android 10 / API 29 / x86。

1. 安装最终 `my_sound_pro.jar`，无外部配置时宿主识别插件及 2 个来源。
2. 写入配置禁用 `librivox`，重启宿主后插件立即变为 1 个来源。
3. 恢复默认配置并启用 Gutenberg，在宿主搜索 `Alice`，返回
   `Alice's Adventures in Wonderland`。
4. 打开详情后解析出 12 章；播放时宿主 `TingShuService` 为 active，
   `PlaybackState=3`，当前媒体为 `19573-01.mp3`，异常日志为空。

## 发布边界

本地生成的 `projectUrl` 仍是 `OWNER` 占位值，因为当前工作区没有用户的 GitHub 仓库身份。
GitHub Actions 发布时会以 `${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}` 覆盖该值，因此正式 Tag
产物会自动写入真实仓库和下载地址。项目未代替维护者创建远程仓库、推送或发布 Release。
