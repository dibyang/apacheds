# ApacheDS Gradle 发布分支

语言：中文 | [English](docs/en/README.md)

本仓库是 Apache Directory Server 的 Gradle 构建与发布整理分支，当前发布版本为 `2.0.0-M27`。本分支固定使用 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M6`，并保留 DIRSERVER-2102 相关并发回归测试。

## 文档

- [CHANGELOG.md](CHANGELOG.md)：版本变更记录。
- [OPEN_SOURCE_RELEASE.md](OPEN_SOURCE_RELEASE.md)：本次开源发布说明。
- [RELEASE.md](RELEASE.md)：正式发布手册。
- [CONTRIBUTING.md](CONTRIBUTING.md)：贡献与验证要求。
- [SECURITY.md](SECURITY.md)：安全漏洞私密报告流程。
- `LICENSE`、`NOTICE`、`DEPENDENCIES`：许可证、声明和依赖信息。

## 反馈入口

- 普通 bug、回归、性能问题和兼容性问题请使用 GitHub issue 的 bug 模板提交。
- 安全漏洞、利用细节、凭据或生产数据不得公开提交，请按 [SECURITY.md](SECURITY.md) 私密报告。
- 贡献代码或文档前，请先阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 构建要求

- JDK 8
- Gradle Wrapper
- Windows PowerShell 或等价 shell

## 常用命令

构建：

```powershell
.\gradlew.bat build
```

装配：

```powershell
.\gradlew.bat assemble
```

发布配置预检：

```powershell
.\gradlew.bat validateReleaseConfiguration
```

本地 Maven 发布验证：

```powershell
.\gradlew.bat publishToMavenLocal
```

DIRSERVER-2102 并发专项测试：

```powershell
.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest
```

## 发布规则

中央仓库发布必须按 [RELEASE.md](RELEASE.md) 执行。默认流程只允许上传到 Sonatype Central Portal 待人工审核；没有维护者明确确认前，不得自动发布到 Maven Central。

## 许可证

本项目遵循 Apache License 2.0。详见 [LICENSE](LICENSE)。
