# 开源发布准备说明

## 当前版本

- 项目版本：`2.0.0-M24V20260126`
- 构建系统：Gradle Wrapper
- JDK 目标版本：Java 8
- JDBM 固定版本：`net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M4`

## 构建方式

项目已经移除 Maven POM，后续以 Gradle 作为唯一构建入口。

```powershell
.\gradlew.bat build
```

DIRSERVER-2102 的 JDBM 并发回归测试保留为独立任务，默认 `build` 不执行：

```powershell
.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest
```

## 发布前检查

- 确认仓库中没有 `pom.xml` 残留，避免 Maven 元数据影响 Gradle 项目识别。
- 确认 `settings.gradle` 中模块列表与本次开源范围一致。
- 确认 `build.gradle` 中的 `projectModel`、`projectDependencies`、`dependencyManagement` 与实际模块依赖一致。
- 执行 `.\gradlew.bat :jdbm-partition:dependencyInsight --dependency apacheds-jdbm1 --configuration compileClasspath`，确认解析到正式版 `2.0.0-M4`。
- 执行 `.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest`，观察 JDBM 并发回归测试是否稳定通过。
- 执行 `.\gradlew.bat build`，确认全量构建通过。
- 检查 `LICENSE`、`NOTICE`、`DEPENDENCIES` 是否覆盖当前第三方依赖。
- 检查 `README.txt` 是否需要补充 Gradle 构建说明、JDK 要求和模块范围。

## 已知测试注意事项

- `jdbm-partition` 的 DIRSERVER-2102 并发回归测试在 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M4` 下仍可能偶发 `EOFException`，路径为 `KeyBTreeCursor.next -> BPage.deserialize -> BaseRecordManager.fetch`。该测试默认不进入 `build`，发布前应多次执行专项任务确认风险窗口。
- `ldap-client-test` 中部分 LDAP 连接测试对本机端口和线程时序敏感。如全量构建偶发失败，可单独重跑 `.\gradlew.bat :ldap-client-test:test` 进行确认。
- `interceptors:subtree` 中属性类型相关测试曾在全量顺序运行中出现过一次失败，单独重跑通过；如果再次出现，应优先排查测试隔离和共享 schema 状态。

## 发布产物建议

- 源码包应包含 Gradle Wrapper、`settings.gradle`、`build.gradle`、`LICENSE`、`NOTICE`、`DEPENDENCIES`、`README.txt` 和本说明。
- 不再发布或维护 Maven POM 作为构建入口。
- 对外说明 JDBM 已固定到 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M4`。该版本可作为当前固定版本使用，但 DIRSERVER-2102 并发回归测试仍需作为发布前风险项持续观察。
