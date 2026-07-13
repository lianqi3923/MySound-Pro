# 长期维护手册

## Nightly 处置

Nightly 使用固定 seed、单请求节流运行线上测试并上传 JSON 报告。失败会创建或更新维护 issue，但不会自动生成 selector、切换域名或加入反爬逻辑。

建议状态：

- `enabled`：fixture 全绿，搜索/详情/章节不低于 90%，播放解析不低于 85%。
- `quarantine`：连续失败、站点改版、网络不稳定或政策待复核；移除注册注解并在配置示例中关闭。
- `rejected`：登录、会员、验证码、Token、签名、DRM 或明确禁止自动访问。

## 故障流程

1. 保存失败 URL、HTTP 状态、耗时、`SourceException.location` 和当天报告。
2. 用现有 fixture 稳定复现；若真实页面已变化，先补新的失败 fixture。
3. 只修改所属 Parser 或公共根因，不在核心层按域名分支。
4. 跑全仓 fixture、D8、发布包和 MyTingShu 宿主回归。
5. 更新准入记录与 CHANGELOG。

## 发布检查

- tag 与 `projectVersion` 一致，`pluginVersionCode` 单调递增。
- `verifyReleaseBundle` 通过，JAR SHA-256 与两个发布 JSON 一致。
- SBOM、许可清单、README、CHANGELOG、LICENSE 齐全。
- Release 默认配置 `debug=false`，无凭据或调试信息泄露。
- 在当前 MyTingShu 完成加载、配置启停、搜索、章节和播放地址回归。
