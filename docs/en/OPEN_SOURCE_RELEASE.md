# Open Source Release Notes

Language: [中文](../../OPEN_SOURCE_RELEASE.md) | English

This document is the release-notes entry for `2.0.0-M27`. The formal release process is defined in [RELEASE.md](RELEASE.md).

## Current Version

- Project version: `2.0.0-M27`
- Build system: Gradle Wrapper
- Target JDK: Java 8
- Fixed JDBM version: `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M6`

## Documentation Entry Points

- [README.md](README.md): project overview, build entry, and usage notes.
- [CHANGELOG.md](CHANGELOG.md): version changelog.
- [RELEASE.md](RELEASE.md): formal release process.
- [CONTRIBUTING.md](CONTRIBUTING.md): contribution and validation requirements.
- [SECURITY.md](SECURITY.md): private security vulnerability reporting process.
- `LICENSE`, `NOTICE`, `DEPENDENCIES`: license, notice, and dependency information.

## Build

Maven POM files have been removed. Gradle is the only supported build entry.

```powershell
.\gradlew.bat build
```

The DIRSERVER-2102 JDBM concurrency regression suite is a dedicated task and is not part of the default `build` lifecycle:

```powershell
.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest
```

## Publication Configuration

Public release configuration is stored in `gradle.properties`, including project versions, internal dependency versions, and POM metadata.

Local secrets must not be committed. Store them in `signing.properties`, Gradle properties, or environment variables:

- `releasesRepository`
- `snapshotsRepository`
- `ossrhUsername`
- `ossrhPassword`
- `signing.keyId`
- `signing.password`
- `signing.secretKeyRingFile`

## Pre-release Checks

Before a formal release, follow the full checklist in [RELEASE.md](RELEASE.md). Common local validation commands are:

```powershell
.\gradlew.bat validateReleaseConfiguration
.\gradlew.bat publishToMavenLocal
.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest
```

If time allows, run the full build:

```powershell
.\gradlew.bat build
```

## Upload and Manual Review

Central publication has two separate phases:

1. Upload to a Sonatype Central Portal deployment for review.
2. Publish/release only after manual review passes.

By default, only upload-for-review is allowed. Do not publish automatically to Maven Central and do not use `publishing_type=automatic` unless the maintainer explicitly authorizes skipping manual review.

## Changes in This Release

- Upgrade JDBM to `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M6`.
- Keep ApacheDS-side `JdbmTable` coordination between redirected-BTree cursor lifetimes and table `put`, `remove`, and `sync`, avoiding stale redirected BTree reads during duplicate value demotion.
- Narrow duplicate-BTree cursor write blocking so ordinary no-duplicate cursors, inline duplicate cursors, and table-level cursors no longer hold the redirected-BTree read lock.
- Optimize JDBM write batching and duplicate-value demotion paths, and add duplicate-BTree lock-wait diagnostics for identifying whether long redirected-BTree cursors still block `put`, `remove`, or `sync`.
- Keep ApacheDS-side DIRSERVER-2102 concurrency regression coverage for demotion/valueCursor, table cursor/update races, shared named table handles, and concurrent table creation.
- Pass through `dirserver2102.*` system properties to Gradle Test tasks for release-time stress tuning.
- Move public version, dependency, and POM metadata to `gradle.properties`.
- Add the formal [RELEASE.md](RELEASE.md) runbook for Central Portal manual review and failure handling.

## Known Test Notes

- The `jdbm-partition` DIRSERVER-2102 concurrency suite is not part of the default `build`; run it before release. The current target release fixes JDBM at `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M6`.
- Some `ldap-client-test` LDAP connection tests are sensitive to local ports and timing. If the full build fails intermittently, rerun `.\gradlew.bat :ldap-client-test:test`.
- An attribute-type test in `interceptors:subtree` failed once during a full sequential build and passed when rerun separately. If it appears again, investigate test isolation and shared schema state first.

## Current Validation

- `.\gradlew.bat validateReleaseConfiguration`: passed, confirming the local release repository URL, credentials, and GPG signing configuration are available.
- `.\gradlew.bat --refresh-dependencies :jdbm-partition:dependencyInsight --dependency apacheds-jdbm1 --configuration compileClasspath`: passed, confirmed `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M6`.
- `.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest --rerun-tasks`: passed.
- `.\gradlew.bat --% :jdbm-partition:jdbmConcurrencyTest --rerun-tasks --tests org.apache.directory.server.core.partition.impl.btree.jdbm.DIRSERVER2102JdbmConcurrencyTest.testConcurrentBTreeDemotionAndValueCursorDoNotReadDeletedBTree -Ddirserver2102.threadCount=24 -Ddirserver2102.iterations=600`: passed.
- `.\gradlew.bat assemble`: passed.
- `.\gradlew.bat publishToMavenLocal`: passed for `2.0.0-M27` POMs, main jars, sources jars, javadoc jars, Gradle module metadata, and `.asc` signatures.
- `.\gradlew.bat build`: not rerun in this round; run it in a terminal without a short timeout if time allows.
- Sonatype Central Portal: `2.0.0-M27` has not been uploaded yet; the next step may only upload it for manual review.

## Release Artifact Recommendation

- Source distributions should include Gradle Wrapper, `settings.gradle`, `build.gradle`, `LICENSE`, `NOTICE`, `DEPENDENCIES`, root Chinese documents, and matching English copies under `docs/en/`.
- Maven POM files are no longer maintained as build entries.
- Public release notes should state that JDBM is fixed to `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M6`, used together with the ApacheDS-side `JdbmTable` coordination and write-blocking optimizations.
