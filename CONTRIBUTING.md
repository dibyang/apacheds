# 贡献指南

语言：中文 | [English](docs/en/CONTRIBUTING.md)

## 基本要求

- 默认使用中文提交说明、文档和关键注释；英文作为辅助文档独立维护。
- 文本文档使用 UTF-8 编码。
- 构建入口使用 Gradle Wrapper，不再使用 Maven POM。
- 修改发布、构建、依赖或持久化相关逻辑时，必须同步更新相关文档。

## 反馈入口

- 普通 bug、回归、性能问题和兼容性问题请使用 GitHub issue 的 bug 模板提交。
- bug 报告应包含版本、JDK、操作系统、存储后端、复现步骤、实际表现、预期表现和相关日志。
- 涉及密码、token、私钥、生产 DN、真实用户数据或可利用细节的问题，不得公开提交；请按 [SECURITY.md](SECURITY.md) 私密报告。
- 不确定是否属于安全问题时，先走安全报告入口。

## 贡献流程

1. 先搜索已有 issue 和变更，避免重复提交。
2. 对行为变更、兼容性变更、持久化或并发相关修改，先在 issue 或 PR 描述中说明方案、风险和回滚方式。
3. 保持 PR 聚焦，一个 PR 只解决一个问题或一组强相关问题。
4. PR 描述应包含变更摘要、验证命令、兼容性影响和未覆盖风险。
5. 不提交本机凭据、签名文件、构建缓存、生产日志或脱敏不完整的数据。

## 开发检查

提交前至少检查：

```powershell
git status --short
git diff --check
```

涉及 Java 代码修改时，应运行最小相关测试或编译任务。涉及 JDBM、BTree、cursor、partition 或 DIRSERVER-2102 时，必须优先运行：

```powershell
.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest
```

只修改 Markdown、GitHub issue 模板或安全政策时，至少运行：

```powershell
git diff --check
```

## 发布相关修改

涉及版本号、发布配置、Sonatype Central、签名、POM 元数据或发布文档时，必须同步检查：

- [RELEASE.md](RELEASE.md)
- [OPEN_SOURCE_RELEASE.md](OPEN_SOURCE_RELEASE.md)
- [CHANGELOG.md](CHANGELOG.md)
- `gradle.properties`

中央仓库发布默认只上传待人工审核。没有明确授权前，不得自动发布到 Maven Central。

## 文档规范

- 中文文档是默认入口。
- 英文副本统一放在 `docs/en/` 目录，不与中文主文档平铺在仓库根目录。
- 中文文档顶部应提供到 `docs/en/` 英文副本的切换链接，英文文档顶部应提供回中文主文档的切换链接。
- 不把英文说明散落在中文段落内部；需要双语时，维护独立语言版本。
