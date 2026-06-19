# Changelog

Language: [中文](CHANGELOG.md) | English

## 2.0.0-M26

- Switch the ApacheDS JDBM dependency to `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`.
- Fix ApacheDS-side `JdbmTable` duplicate BTree demotion races by coordinating cursor lifetimes with `put`, `remove`, and `sync` through a table-level read-write lock.
- Add DIRSERVER-2102 concurrency regression coverage for demotion/valueCursor, table cursor/update races, shared named table handles, and concurrent table creation.
- Keep `dirserver2102.*` test properties pass-through in Gradle so release validation can raise thread counts and iteration counts without code changes.
- Add Gradle `maven-publish` and `signing` configuration for Maven Central staging publication.
- Move public release settings such as project versions and POM metadata into `gradle.properties`.
- Add the formal [RELEASE.en.md](RELEASE.en.md) runbook and update [OPEN_SOURCE_RELEASE.en.md](OPEN_SOURCE_RELEASE.en.md) as a Chinese-primary, English-switchable release notes entry.
- Add [README.en.md](README.en.md), [CONTRIBUTING.en.md](CONTRIBUTING.en.md), and matching Chinese documents to provide standard open source entry points.
