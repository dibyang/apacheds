# Changelog

## 2.0.0-M26

> 中文为主，英文为辅。English notes are provided as secondary references.

- 将 ApacheDS JDBM 依赖切换为 `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`。
  English note: switch the ApacheDS JDBM dependency to the fixed M5 build.
- 修复 ApacheDS 侧 `JdbmTable` duplicate BTree demotion 并发问题，通过表级读写锁协调 cursor 生命周期与 `put`、`remove`、`sync`。
  English note: coordinate cursor lifetimes with table updates to avoid stale redirected BTree reads.
- 增加 DIRSERVER-2102 并发回归覆盖，包括 demotion/valueCursor、表级 cursor 与更新、共享 named table handle、并发表创建等场景。
  English note: add ApacheDS-side concurrency regression coverage for DIRSERVER-2102.
- 保留 `dirserver2102.*` 测试属性透传，发布验证时可直接提高线程数和迭代次数。
  English note: keep configurable stress parameters for release validation.
- 增加 Gradle `maven-publish` 与 `signing` 配置，支持 Maven Central staging 上传。
  English note: add Gradle publication and signing configuration for Central staging.
- 将公开发布配置集中到 `gradle.properties`，包括项目版本、内部依赖版本和 POM 元数据。
  English note: centralize public release metadata in `gradle.properties`.
- 新增 [RELEASE.md](RELEASE.md) 正式发布手册，并调整 [OPEN_SOURCE_RELEASE.md](OPEN_SOURCE_RELEASE.md) 为中文主线、英文辅助的发布说明入口。
  English note: add a release runbook and bilingual release documentation entry point.
