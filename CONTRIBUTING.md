# 贡献指南

语言：中文 | [English](docs/en/CONTRIBUTING.md)

## 基本要求

- 默认使用中文提交说明、文档和关键注释；英文作为辅助文档独立维护。
- 文本文档使用 UTF-8 编码。
- 构建入口使用 Gradle Wrapper，不再使用 Maven POM。
- 修改发布、构建、依赖或持久化相关逻辑时，必须同步更新相关文档。

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
