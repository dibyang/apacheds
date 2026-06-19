# 开源发布准备说明

## 当前版本

- 项目版本：`2.0.0-M24V20260126`
- 构建系统：Gradle Wrapper
- JDK 目标版本：Java 8
- JDBM 固定版本：`net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`

## 构建方式

项目已经移除 Maven POM，后续以 Gradle 作为唯一构建入口。

```powershell
.\gradlew.bat build
```

DIRSERVER-2102 的 JDBM 并发回归测试保留为独立任务，默认 `build` 不执行：

```powershell
.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest
```

公开发布配置位于 `gradle.properties`，包括项目版本、内部依赖版本和 POM 元数据。发布到 Maven Central / Sonatype 兼容仓库前，还需在本地准备 `signing.properties` 或等价 Gradle 属性/环境变量：

- `releasesRepository`
- `snapshotsRepository`
- `ossrhUsername`
- `ossrhPassword`
- `signing.keyId`
- `signing.password`
- `signing.secretKeyRingFile`

本地发布验证：

```powershell
.\gradlew.bat publishToMavenLocal
```

发布到中央仓库 staging：

```powershell
.\gradlew.bat publishAllPublicationsToSonatypeCentralRepository
```

该命令负责上传全部模块的 Maven publication。staging close/release 仍需通过 Sonatype Central Portal 或对应 API 完成。

## 发布前检查

- 确认仓库中没有 `pom.xml` 残留，避免 Maven 元数据影响 Gradle 项目识别。
- 确认 `settings.gradle` 中模块列表与本次开源范围一致。
- 确认 `build.gradle` 中的 `projectModel`、`projectDependencies`、`dependencyManagement` 与实际模块依赖一致。
- 执行 `.\gradlew.bat :jdbm-partition:dependencyInsight --dependency apacheds-jdbm1 --configuration compileClasspath`，确认解析到正式版 `2.0.0-M5`。
- 执行 `.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest`，观察 JDBM 并发回归测试是否稳定通过。
- 执行 `.\gradlew.bat publishToMavenLocal`，确认所有模块可以生成 POM、主 jar、sources jar、javadoc jar 和 `.asc` 签名。
- 执行 `.\gradlew.bat build`，确认全量构建通过。
- 检查 `LICENSE`、`NOTICE`、`DEPENDENCIES` 是否覆盖当前第三方依赖。
- 检查 `README.txt` 是否需要补充 Gradle 构建说明、JDK 要求和模块范围。
- 确认本地签名配置 `signing.properties` 不入库。

## 本次发布变更

- JDBM 依赖升级到 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`。
- `JdbmTable` 在 `valueCursor(key)` / `cursor(key)` / `cursor()` 与同表 `put`、`remove`、`sync` 之间增加读写锁互斥，避免 duplicate value demotion 删除 redirected BTree 时，并发 cursor 仍通过旧 recid 加载已删除记录。
- DIRSERVER-2102 并发回归测试增加 ApacheDS 侧覆盖，包括 demotion/valueCursor、表级 cursor 与更新、共享 named table handle、并发表创建等场景。
- `dirserver2102.*` 系统属性会透传到 Gradle Test 任务，可用于发布前提高线程数和迭代次数进行专项加压。

## 已知测试注意事项

- `jdbm-partition` 的 DIRSERVER-2102 并发回归测试默认不进入 `build`，发布前应执行专项任务确认 JDBM 并发路径。当前在 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5` 下，`jdbmConcurrencyTest` 和 demotion/valueCursor 加压用例均已通过。
- `ldap-client-test` 中部分 LDAP 连接测试对本机端口和线程时序敏感。如全量构建偶发失败，可单独重跑 `.\gradlew.bat :ldap-client-test:test` 进行确认。
- `interceptors:subtree` 中属性类型相关测试曾在全量顺序运行中出现过一次失败，单独重跑通过；如果再次出现，应优先排查测试隔离和共享 schema 状态。

## 当前验证结果

- `.\gradlew.bat --refresh-dependencies :jdbm-partition:dependencyInsight --dependency apacheds-jdbm1 --configuration compileClasspath`：通过，确认解析到 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`。
- `.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest --rerun-tasks`：通过。
- `.\gradlew.bat --% :jdbm-partition:jdbmConcurrencyTest --rerun-tasks --tests org.apache.directory.server.core.partition.impl.btree.jdbm.DIRSERVER2102JdbmConcurrencyTest.testConcurrentBTreeDemotionAndValueCursorDoNotReadDeletedBTree -Ddirserver2102.threadCount=24 -Ddirserver2102.iterations=600`：通过。
- `.\gradlew.bat assemble`：通过，确认所有模块产物可装配。
- `.\gradlew.bat publishToMavenLocal`：通过，确认所有模块 publication、POM 元数据、sources/javadoc jar 和 `.asc` 签名可生成。
- `.\gradlew.bat build`：执行两次，分别超过 15 分钟和 30 分钟后由当前工具超时中断，未得到有效失败日志；正式发布前仍建议在不受工具超时限制的环境中补跑全量构建。

## 发布产物建议

- 源码包应包含 Gradle Wrapper、`settings.gradle`、`build.gradle`、`LICENSE`、`NOTICE`、`DEPENDENCIES`、`README.txt` 和本说明。
- 不再发布或维护 Maven POM 作为构建入口。
- 对外说明 JDBM 已固定到 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`。该版本配合 ApacheDS 侧 `JdbmTable` 互斥修复，作为当前固定版本使用。
