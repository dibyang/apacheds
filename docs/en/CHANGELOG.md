# Changelog

Language: [中文](../../CHANGELOG.md) | English

## 2.0.0-M27-SNAPSHOT

- Start the next development cycle as `2.0.0-M27-SNAPSHOT`.
- Add `JdbmTable` duplicate BTree lock-wait diagnostics to identify whether long redirected-BTree cursors block `put`, `remove`, or `sync`; the default threshold is 1000 ms and can be disabled with `-Djdbm.table.duplicate.btree.lock.warn.millis=0`.

## 2.0.0-M26

- Switch the ApacheDS JDBM dependency to `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`.
- Fix ApacheDS-side `JdbmTable` duplicate BTree demotion races by coordinating cursor lifetimes with `put`, `remove`, and `sync` through a table-level read-write lock.
- Add DIRSERVER-2102 concurrency regression coverage for demotion/valueCursor, table cursor/update races, shared named table handles, and concurrent table creation.
- Keep `dirserver2102.*` test properties pass-through in Gradle so release validation can raise thread counts and iteration counts without code changes.
- Add Gradle `maven-publish` and `signing` configuration for Maven Central staging publication.
- Move public release settings such as project versions and POM metadata into `gradle.properties`.
- Add the formal [RELEASE.md](RELEASE.md) runbook and update [OPEN_SOURCE_RELEASE.md](OPEN_SOURCE_RELEASE.md) as the English release notes copy under `docs/en/`.
- Add [README.md](README.md), [CONTRIBUTING.md](CONTRIBUTING.md), and matching Chinese documents to provide standard open source entry points.
