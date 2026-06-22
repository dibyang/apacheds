# 变更日志

语言：中文 | [English](docs/en/CHANGELOG.md)

## 2.0.0-M27-SNAPSHOT

- 进入 `2.0.0-M27-SNAPSHOT` 下一轮开发版本。
- 增加 `JdbmTable` duplicate BTree 锁等待诊断日志，用于定位 redirected BTree 长 cursor 是否阻塞 `put`、`remove`、`sync`；默认阈值为 1000 ms，可通过 `-Djdbm.table.duplicate.btree.lock.warn.millis=0` 关闭。

## 2.0.0-M26

- 将 ApacheDS JDBM 依赖切换为 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`。
- 修复 ApacheDS 侧 `JdbmTable` duplicate BTree demotion 并发问题，通过表级读写锁协调 cursor 生命周期与 `put`、`remove`、`sync`。
- 增加 DIRSERVER-2102 并发回归覆盖，包括 demotion/valueCursor、表级 cursor 与更新、共享 named table handle、并发表创建等场景。
- 保留 `dirserver2102.*` 测试属性透传，发布验证时可直接提高线程数和迭代次数。
- 增加 Gradle `maven-publish` 与 `signing` 配置，支持 Maven Central staging 上传。
- 将公开发布配置集中到 `gradle.properties`，包括项目版本、内部依赖版本和 POM 元数据。
- 新增 [RELEASE.md](RELEASE.md) 正式发布手册，并调整 [OPEN_SOURCE_RELEASE.md](OPEN_SOURCE_RELEASE.md) 为中文主线、英文独立切换的发布说明入口。
- 新增 [README.md](README.md)、[CONTRIBUTING.md](CONTRIBUTING.md) 及对应英文文档，补齐开源项目入口文档。
