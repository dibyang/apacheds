# 发布手册 / Release Runbook

> 本文是正式发布必须遵循的操作手册。中文为主，英文为辅。
>
> This is the required release runbook. Chinese is primary; English is secondary.

## 1. 发布原则 / Release Principles

- 发布动作必须作用于当前仓库：`D:\work\java\apacheds`。
- Maven POM 已移除，Gradle Wrapper 是唯一构建和发布入口。
- 版本号、内部依赖版本和公开 POM 元数据统一配置在 `gradle.properties`。
- 敏感信息只允许放在本机 `signing.properties`、Gradle 用户属性或环境变量中，不得入库。
- 中央仓库发布必须先上传到 Sonatype Central Portal 等待人工审核；未经明确授权，不得自动 publish/release。
- 如果同一版本出现部分模块已进入 Maven Central、部分模块未发布，立即停止使用该版本，升级到新版本号重新走完整流程。

English note: always use Gradle, keep secrets local, and separate upload-for-review from final publication.

## 2. 版本准备 / Version Preparation

1. 更新 `gradle.properties`：

```properties
projectVersion=2.0.0-M26
jdbmVersion=2.0.0-M5
```

2. 确认 `build.gradle` 中的 fallback 版本号与 `projectVersion` 一致。
3. 更新 `CHANGELOG.md`，记录面向使用者的变更。
4. 更新 `OPEN_SOURCE_RELEASE.md`，记录本次验证结果、已知风险和发布说明。
5. 如发布流程或审核要求变化，同步更新本文件。

English note: keep version metadata, changelog, and release notes in sync before any upload.

## 3. 发布前本地检查 / Local Checks

确认没有 Maven POM 残留：

```powershell
Get-ChildItem -Recurse -Filter pom.xml
```

确认工作区变更范围：

```powershell
git status --short
git diff --name-only
```

确认发布配置完整：

```powershell
.\gradlew.bat validateReleaseConfiguration
```

确认 JDBM 依赖解析到固定版本：

```powershell
.\gradlew.bat --refresh-dependencies :jdbm-partition:dependencyInsight --dependency apacheds-jdbm1 --configuration compileClasspath
```

运行 DIRSERVER-2102 并发专项测试：

```powershell
.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest --rerun-tasks
```

运行 demotion/valueCursor 加压用例：

```powershell
.\gradlew.bat --% :jdbm-partition:jdbmConcurrencyTest --rerun-tasks --tests org.apache.directory.server.core.partition.impl.btree.jdbm.DIRSERVER2102JdbmConcurrencyTest.testConcurrentBTreeDemotionAndValueCursorDoNotReadDeletedBTree -Ddirserver2102.threadCount=24 -Ddirserver2102.iterations=600
```

确认所有模块可装配：

```powershell
.\gradlew.bat assemble
```

确认本地 Maven 发布产物、POM、sources、javadoc 和签名可生成：

```powershell
.\gradlew.bat publishToMavenLocal
```

如时间允许，在不受短超时影响的终端补跑全量构建：

```powershell
.\gradlew.bat build
```

English note: release-critical checks are configuration validation, dependency insight, JDBM concurrency tests, assembly, and local Maven publication.

## 4. 上传到 Central Portal 待审核 / Upload for Review

上传前再次确认没有残留发布进程：

```powershell
Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'gradle|publishAllPublications|SonatypeCentral|central.sonatype' } | Select-Object ProcessId,Name,CommandLine
```

上传全部模块到 Sonatype Central 的 staging repository：

```powershell
.\gradlew.bat publishAllPublicationsToSonatypeCentralRepository --console=plain
```

上传完成后必须记录：

- repository key
- deployment id
- Portal 状态
- errors
- warnings
- purls 中的版本号

English note: upload all modules together and record the repository key, deployment id, validation state, errors, warnings, and artifact versions.

## 5. 人工审核闸门 / Manual Review Gate

上传后只允许进入待审核状态。默认使用 Central Portal 的 user-managed 流程。

人工审核至少检查：

- Portal deployment state 为 `VALIDATED`。
- `errors` 为空。
- 所有 `purls` 都是本次版本号，例如 `2.0.0-M26`。
- 没有混入已废弃版本，例如 `2.0.0-M25`。
- 关键模块均存在，例如 `apacheds-jdbm-partition`、`apacheds-ldif-partition`、`apacheds-core-api`、`apacheds-service`、`apacheds-all`。
- POM 中内部 ApacheDS 依赖版本一致。
- POM 中 `net.xdob.directory.jdbm:apacheds-jdbm1` 固定为 `2.0.0-M5`。
- sources jar、javadoc jar 和 `.asc` 签名齐全。
- `LICENSE`、`NOTICE`、`DEPENDENCIES` 和 POM license/scm/developer 元数据合理。

没有人工确认前，不得执行最终 publish/release。

English note: do not publish to Maven Central until a human has checked the validated deployment.

## 6. 最终发布 / Final Publication

只有维护者明确说“人工审核通过，可以正式发布到 Maven Central”后，才能执行最终 publish/release。

禁止把“正式发布”默认理解为自动发布。如果上下文不清楚，必须先确认：

- 是只上传待审核，还是发布到 Maven Central 可见？
- 是否允许跳过人工审核？
- 是否确认当前 deployment id？

English note: final publication requires explicit human approval after review.

## 7. 异常处理 / Failure Handling

如果上传或发布被中断：

1. 立即检查后台 Gradle/Java 进程。
2. 如仍在上传，记录 PID 和命令行，先不要重复执行发布命令。
3. 查询 Central Portal 当前 repository/deployment 状态。
4. 确认是否存在部分模块成功。

如果某版本已经部分进入 Maven Central：

1. 停止继续使用该版本。
2. 记录已发布坐标和未发布坐标。
3. 不尝试覆盖、删除或补齐同一版本。
4. 升级到新版本号重新执行完整流程。

如果 Central Portal 中存在旧的 open/closed staging repository：

1. 先确认该 repository 属于废弃版本或异常上传。
2. 经维护者确认后 drop。
3. drop 后重新查询，确认列表清空或状态符合预期。

English note: partial Central publication pollutes the version; abandon it and move to a new version.

## 8. M26 当前状态 / Current M26 State

- 版本：`2.0.0-M26`
- JDBM：`net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`
- Central Portal deployment id：`c7357a79-8d74-48dd-a00e-11028916c4e7`
- Portal 状态：`VALIDATED`
- 错误：无
- 当前要求：等待人工审核，不自动发布。

English note: M26 has been uploaded and validated, and is waiting for manual review.
