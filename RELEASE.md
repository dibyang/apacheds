# 发布手册

语言：中文 | [English](docs/en/RELEASE.md)

本文是本项目正式发布必须遵循的操作手册。默认流程是“上传到 Sonatype Central Portal 待人工审核”，不是自动发布到 Maven Central。

## 1. 发布原则

- 发布动作必须作用于当前仓库检出目录。
- Maven POM 已移除，Gradle Wrapper 是唯一构建和发布入口。
- 版本号、内部依赖版本和公开 POM 元数据统一配置在 `gradle.properties`。
- 敏感信息只允许放在本机 `signing.properties`、Gradle 用户属性或环境变量中，不得入库。
- 中央仓库发布必须先上传到 Sonatype Central Portal 等待人工审核。
- 未经维护者明确授权，不得自动 publish/release，不得使用 `publishing_type=automatic`。
- 如果同一版本出现部分模块已进入 Maven Central、部分模块未发布，立即停止使用该版本，升级到新版本号重新走完整流程。

## 2. 版本准备

更新 `gradle.properties`：

```properties
projectVersion=<待发布版本>
jdbmVersion=2.0.0-M6
```

发布前必须同步检查：

- `build.gradle` 中的 fallback 版本号与 `projectVersion` 一致。
- [CHANGELOG.md](CHANGELOG.md) 已记录面向使用者的变更。
- [OPEN_SOURCE_RELEASE.md](OPEN_SOURCE_RELEASE.md) 已记录本次验证结果、已知风险和发布说明。
- 本文件已反映最新发布流程。
- 不存在应移除的 `pom.xml` 残留。

## 3. 本地检查

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

## 4. 上传到 Central Portal 待审核

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

## 5. 人工审核闸门

上传后只允许进入待审核状态。默认使用 Central Portal 的 user-managed 流程。

人工审核至少检查：

- Portal deployment state 为 `VALIDATED`。
- `errors` 为空。
- 所有 `purls` 都是本次发布版本号。
- 没有混入已废弃版本，例如 `2.0.0-M25`。
- 关键模块均存在，例如 `apacheds-jdbm-partition`、`apacheds-ldif-partition`、`apacheds-core-api`、`apacheds-service`、`apacheds-all`。
- POM 中内部 ApacheDS 依赖版本一致。
- POM 中 `net.xdob.directory.jdbm:apacheds-jdbm1` 固定为 `2.0.0-M6`。
- sources jar、javadoc jar 和 `.asc` 签名齐全。
- `LICENSE`、`NOTICE`、`DEPENDENCIES` 和 POM license/scm/developer 元数据合理。

没有人工确认前，不得执行最终 publish/release。

## 6. 最终发布

只有维护者明确说“人工审核通过，可以正式发布到 Maven Central”后，才能执行最终 publish/release。

如果上下文不清楚，必须先确认：

- 是只上传待审核，还是发布到 Maven Central 可见？
- 是否允许跳过人工审核？
- 是否确认当前 deployment id？

## 7. 异常处理

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
