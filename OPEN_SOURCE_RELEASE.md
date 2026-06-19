# 开源发布准备说明 / Open Source Release Notes

> 本文中文为主，英文为辅。需要英文核查时，可阅读每个小节下的 `English note`。
>
> Chinese is the primary language in this document. English notes are provided as a secondary reference.

## 当前版本 / Current Version

- 项目版本：`2.0.0-M26`
- 构建系统：Gradle Wrapper
- JDK 目标版本：Java 8
- JDBM 固定版本：`net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`

English note: this release candidate is version `2.0.0-M26` and uses Gradle as the only supported build entry.

## 文档入口 / Documentation Entry Points

- [RELEASE.md](RELEASE.md)：正式发布流程，包含发布前检查、上传待审核、人工审核、最终发布和异常处理。
- [CHANGELOG.md](CHANGELOG.md)：版本变更记录。
- [README.txt](README.txt)：项目原始说明。
- `LICENSE`、`NOTICE`、`DEPENDENCIES`：开源发布必需的许可证、声明和依赖信息。

English note: `RELEASE.md` is the authoritative release runbook for this Gradle-based publication flow.

## 构建方式 / Build

项目已经移除 Maven POM，后续以 Gradle 作为唯一构建入口。

```powershell
.\gradlew.bat build
```

DIRSERVER-2102 的 JDBM 并发回归测试保留为独立任务，默认 `build` 不执行：

```powershell
.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest
```

English note: Maven POM files are no longer build entry points. Use Gradle Wrapper commands only.

## 发布配置 / Publication Configuration

公开发布配置位于 `gradle.properties`，包括项目版本、内部依赖版本和 POM 元数据。

本机敏感发布配置不得入库，应放在 `signing.properties` 或等价 Gradle 属性/环境变量中：

- `releasesRepository`
- `snapshotsRepository`
- `ossrhUsername`
- `ossrhPassword`
- `signing.keyId`
- `signing.password`
- `signing.secretKeyRingFile`

English note: public metadata lives in `gradle.properties`; credentials and signing secrets must stay local.

## 发布前检查 / Pre-release Checks

正式发布前必须按 [RELEASE.md](RELEASE.md) 执行完整清单。常用本地验证命令如下：

```powershell
.\gradlew.bat validateReleaseConfiguration
.\gradlew.bat publishToMavenLocal
.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest
```

如时间允许，应补跑全量构建：

```powershell
.\gradlew.bat build
```

English note: run release configuration validation, local Maven publication, and the JDBM concurrency regression test before upload.

## 上传与人工审核 / Upload and Manual Review

中央仓库发布必须区分两个阶段：

1. 上传到 Sonatype Central Portal 的待审核 deployment。
2. 人工核查通过后，再明确执行 publish/release。

默认只允许上传待审核，不得自动发布到 Maven Central。除非维护者明确授权“跳过人工审核并自动发布”，否则不得使用 `publishing_type=automatic`。

English note: upload-for-review and final publication are separate steps. The default flow is user-managed manual review.

## 本次发布变更 / Changes in This Release

- JDBM 依赖升级到 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`。
- `JdbmTable` 在 `valueCursor(key)` / `cursor(key)` / `cursor()` 与同表 `put`、`remove`、`sync` 之间增加读写锁互斥，避免 duplicate value demotion 删除 redirected BTree 时，并发 cursor 仍通过旧 recid 加载已删除记录。
- DIRSERVER-2102 并发回归测试增加 ApacheDS 侧覆盖，包括 demotion/valueCursor、表级 cursor 与更新、共享 named table handle、并发表创建等场景。
- `dirserver2102.*` 系统属性会透传到 Gradle Test 任务，可用于发布前提高线程数和迭代次数进行专项加压。
- 发布相关版本号、依赖版本和 POM 元数据集中到 `gradle.properties`。
- 新增正式发布手册 [RELEASE.md](RELEASE.md)，明确 Central Portal 人工审核流程和异常处理规则。

English note: this release combines the JDBM M5 dependency, ApacheDS-side concurrency protection, and a documented manual-review release process.

## 已知测试注意事项 / Test Notes

- `jdbm-partition` 的 DIRSERVER-2102 并发回归测试默认不进入 `build`，发布前应执行专项任务确认 JDBM 并发路径。当前在 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5` 下，`jdbmConcurrencyTest` 和 demotion/valueCursor 加压用例均已通过。
- `ldap-client-test` 中部分 LDAP 连接测试对本机端口和线程时序敏感。如全量构建偶发失败，可单独重跑 `.\gradlew.bat :ldap-client-test:test` 进行确认。
- `interceptors:subtree` 中属性类型相关测试曾在全量顺序运行中出现过一次失败，单独重跑通过；如果再次出现，应优先排查测试隔离和共享 schema 状态。

English note: the JDBM concurrency suite is release-critical but intentionally separated from the default `build` lifecycle.

## 当前验证结果 / Current Validation

- `.\gradlew.bat --refresh-dependencies :jdbm-partition:dependencyInsight --dependency apacheds-jdbm1 --configuration compileClasspath`：通过，确认解析到 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`。
- `.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest --rerun-tasks`：通过。
- `.\gradlew.bat --% :jdbm-partition:jdbmConcurrencyTest --rerun-tasks --tests org.apache.directory.server.core.partition.impl.btree.jdbm.DIRSERVER2102JdbmConcurrencyTest.testConcurrentBTreeDemotionAndValueCursorDoNotReadDeletedBTree -Ddirserver2102.threadCount=24 -Ddirserver2102.iterations=600`：通过。
- `.\gradlew.bat assemble`：通过，确认所有模块产物可装配。
- `.\gradlew.bat validateReleaseConfiguration`：通过，确认当前本机中央仓库地址、账号和 GPG 签名配置可用。
- `.\gradlew.bat publishToMavenLocal`：通过，确认 `2.0.0-M26` 下所有模块的 POM、主 jar、sources/javadoc jar、Gradle module metadata 和 `.asc` 签名可生成。
- `.\gradlew.bat build`：执行两次，分别超过 15 分钟和 30 分钟后由当前工具超时中断，未得到有效失败日志；正式发布前仍建议在不受工具超时限制的环境中补跑全量构建。
- Sonatype Central Portal：`2.0.0-M26` 已上传并校验为 `VALIDATED`，deployment id 为 `c7357a79-8d74-48dd-a00e-11028916c4e7`，等待人工审核后再发布。

English note: the current Central Portal deployment has been validated and is awaiting manual review.

## 发布产物建议 / Release Artifacts

- 源码包应包含 Gradle Wrapper、`settings.gradle`、`build.gradle`、`LICENSE`、`NOTICE`、`DEPENDENCIES`、`README.txt`、`CHANGELOG.md`、`OPEN_SOURCE_RELEASE.md` 和 `RELEASE.md`。
- 不再发布或维护 Maven POM 作为构建入口。
- 对外说明 JDBM 已固定到 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`。该版本配合 ApacheDS 侧 `JdbmTable` 互斥修复，作为当前固定版本使用。

English note: release source distributions should include the Gradle build files and the Chinese-first release documentation.
