# 开源发布说明

语言：中文 | [English](docs/en/OPEN_SOURCE_RELEASE.md)

本文是 `2.0.0-M27` 的开源发布说明入口。正式发布操作以 [RELEASE.md](RELEASE.md) 为准。

## 当前版本

- 项目版本：`2.0.0-M27`
- 构建系统：Gradle Wrapper
- JDK 目标版本：Java 8
- JDBM 固定版本：`net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M6`

## 文档入口

- [README.md](README.md)：项目介绍、构建方式和使用入口。
- [CHANGELOG.md](CHANGELOG.md)：版本变更记录。
- [RELEASE.md](RELEASE.md)：正式发布流程。
- [CONTRIBUTING.md](CONTRIBUTING.md)：贡献与验证要求。
- [SECURITY.md](SECURITY.md)：安全漏洞私密报告流程。
- `LICENSE`、`NOTICE`、`DEPENDENCIES`：许可证、声明和依赖信息。

## 构建方式

项目已经移除 Maven POM，后续以 Gradle 作为唯一构建入口。

```powershell
.\gradlew.bat build
```

DIRSERVER-2102 的 JDBM 并发回归测试保留为独立任务，默认 `build` 不执行：

```powershell
.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest
```

## 发布配置

公开发布配置位于 `gradle.properties`，包括项目版本、内部依赖版本和 POM 元数据。

本机敏感发布配置不得入库，应放在 `signing.properties` 或等价 Gradle 属性/环境变量中：

- `releasesRepository`
- `snapshotsRepository`
- `ossrhUsername`
- `ossrhPassword`
- `signing.keyId`
- `signing.password`
- `signing.secretKeyRingFile`

## 发布前检查

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

## 上传与人工审核

中央仓库发布必须区分两个阶段：

1. 上传到 Sonatype Central Portal 的待审核 deployment。
2. 人工核查通过后，再明确执行 publish/release。

默认只允许上传待审核，不得自动发布到 Maven Central。除非维护者明确授权“跳过人工审核并自动发布”，否则不得使用 `publishing_type=automatic`。

## 本次发布变更

- JDBM 依赖升级到 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M6`。
- 保留 `JdbmTable` 在 redirected BTree cursor 生命周期与同表 `put`、`remove`、`sync` 之间的互斥，避免 duplicate value demotion 删除 redirected BTree 时，并发 cursor 仍通过旧 recid 加载已删除记录。
- 缩小 duplicate BTree cursor 写入阻塞范围，普通无重复值 cursor、内联重复值 cursor 和表级 cursor 不再持有 redirected BTree 读锁，降低 LDAP 高频写入场景的阻塞风险。
- 优化 JDBM 写入批处理与重复值降级路径，并增加 duplicate BTree 锁等待诊断日志，便于定位 redirected BTree 长 cursor 是否仍阻塞 `put`、`remove`、`sync`。
- DIRSERVER-2102 并发回归测试继续覆盖 ApacheDS 侧 demotion/valueCursor、表级 cursor 与更新、共享 named table handle、并发表创建等场景。
- `dirserver2102.*` 系统属性会透传到 Gradle Test 任务，可用于发布前提高线程数和迭代次数进行专项加压。
- 发布相关版本号、依赖版本和 POM 元数据集中到 `gradle.properties`。
- 新增正式发布手册 [RELEASE.md](RELEASE.md)，明确 Central Portal 人工审核流程和异常处理规则。

## 已知测试注意事项

- `jdbm-partition` 的 DIRSERVER-2102 并发回归测试默认不进入 `build`，发布前应执行专项任务确认 JDBM 并发路径。当前目标版本固定为 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M6`。
- `ldap-client-test` 中部分 LDAP 连接测试对本机端口和线程时序敏感。如全量构建偶发失败，可单独重跑 `.\gradlew.bat :ldap-client-test:test` 进行确认。
- `interceptors:subtree` 中属性类型相关测试曾在全量顺序运行中出现过一次失败，单独重跑通过；如果再次出现，应优先排查测试隔离和共享 schema 状态。

## 当前验证结果

- `.\gradlew.bat validateReleaseConfiguration`：通过，确认当前本机中央仓库 release 地址、账号和 GPG 签名配置可用。
- `.\gradlew.bat --refresh-dependencies :jdbm-partition:dependencyInsight --dependency apacheds-jdbm1 --configuration compileClasspath`：通过，确认解析到 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M6`。
- `.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest --rerun-tasks`：通过。
- `.\gradlew.bat --% :jdbm-partition:jdbmConcurrencyTest --rerun-tasks --tests org.apache.directory.server.core.partition.impl.btree.jdbm.DIRSERVER2102JdbmConcurrencyTest.testConcurrentBTreeDemotionAndValueCursorDoNotReadDeletedBTree -Ddirserver2102.threadCount=24 -Ddirserver2102.iterations=600`：通过。
- `.\gradlew.bat assemble`：通过，确认所有模块产物可装配。
- `.\gradlew.bat publishToMavenLocal`：通过，确认 `2.0.0-M27` 下所有模块的 POM、主 jar、sources/javadoc jar、Gradle module metadata 和 `.asc` 签名可生成。
- `.\gradlew.bat build`：本轮尚未补跑；如时间允许，在不受短超时影响的终端补跑。
- Sonatype Central Portal：尚未上传 `2.0.0-M27`，后续只能上传到待人工审核状态。

## 发布产物建议

- 源码包应包含 Gradle Wrapper、`settings.gradle`、`build.gradle`、`LICENSE`、`NOTICE`、`DEPENDENCIES`、`README.md`、`CHANGELOG.md`、`OPEN_SOURCE_RELEASE.md`、`RELEASE.md` 和 `docs/en/` 英文文档副本。
- 不再发布或维护 Maven POM 作为构建入口。
- 对外说明 JDBM 已固定到 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M6`。该版本配合 ApacheDS 侧 `JdbmTable` 互斥与写入阻塞优化，作为当前固定版本使用。
