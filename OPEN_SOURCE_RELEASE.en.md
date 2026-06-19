# Open Source Release Notes

Language: [中文](OPEN_SOURCE_RELEASE.md) | English

This document is the release-notes entry for `2.0.0-M26`. The formal release process is defined in [RELEASE.en.md](RELEASE.en.md).

## Current Version

- Project version: `2.0.0-M26`
- Build system: Gradle Wrapper
- Target JDK: Java 8
- Fixed JDBM version: `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`

## Documentation Entry Points

- [README.en.md](README.en.md): project overview, build entry, and usage notes.
- [CHANGELOG.en.md](CHANGELOG.en.md): version changelog.
- [RELEASE.en.md](RELEASE.en.md): formal release process.
- [CONTRIBUTING.en.md](CONTRIBUTING.en.md): contribution and validation requirements.
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

Before a formal release, follow the full checklist in [RELEASE.en.md](RELEASE.en.md). Common local validation commands are:

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

- Upgrade JDBM to `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`.
- Add ApacheDS-side `JdbmTable` read-write lock coordination between `valueCursor(key)` / `cursor(key)` / `cursor()` and table `put`, `remove`, and `sync`, avoiding stale redirected BTree reads during duplicate value demotion.
- Add ApacheDS-side DIRSERVER-2102 concurrency regression coverage for demotion/valueCursor, table cursor/update races, shared named table handles, and concurrent table creation.
- Pass through `dirserver2102.*` system properties to Gradle Test tasks for release-time stress tuning.
- Move public version, dependency, and POM metadata to `gradle.properties`.
- Add the formal [RELEASE.en.md](RELEASE.en.md) runbook for Central Portal manual review and failure handling.

## Known Test Notes

- The `jdbm-partition` DIRSERVER-2102 concurrency suite is not part of the default `build`; run it before release. With `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`, `jdbmConcurrencyTest` and the demotion/valueCursor stress case have passed.
- Some `ldap-client-test` LDAP connection tests are sensitive to local ports and timing. If the full build fails intermittently, rerun `.\gradlew.bat :ldap-client-test:test`.
- An attribute-type test in `interceptors:subtree` failed once during a full sequential build and passed when rerun separately. If it appears again, investigate test isolation and shared schema state first.

## Current Validation

- `.\gradlew.bat --refresh-dependencies :jdbm-partition:dependencyInsight --dependency apacheds-jdbm1 --configuration compileClasspath`: passed, confirmed `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`.
- `.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest --rerun-tasks`: passed.
- `.\gradlew.bat --% :jdbm-partition:jdbmConcurrencyTest --rerun-tasks --tests org.apache.directory.server.core.partition.impl.btree.jdbm.DIRSERVER2102JdbmConcurrencyTest.testConcurrentBTreeDemotionAndValueCursorDoNotReadDeletedBTree -Ddirserver2102.threadCount=24 -Ddirserver2102.iterations=600`: passed.
- `.\gradlew.bat assemble`: passed.
- `.\gradlew.bat validateReleaseConfiguration`: passed.
- `.\gradlew.bat publishToMavenLocal`: passed for `2.0.0-M26` POMs, main jars, sources jars, javadoc jars, Gradle module metadata, and `.asc` signatures.
- `.\gradlew.bat build`: interrupted twice by the current tool timeout after 15 and 30 minutes without a useful failure log. Run it in a terminal without a short timeout before final publication if possible.
- Sonatype Central Portal: `2.0.0-M26` was uploaded and validated as `VALIDATED`; deployment id is `c7357a79-8d74-48dd-a00e-11028916c4e7`; it is waiting for manual review.

## Release Artifact Recommendation

- Source distributions should include Gradle Wrapper, `settings.gradle`, `build.gradle`, `LICENSE`, `NOTICE`, `DEPENDENCIES`, `README.md`, `CHANGELOG.md`, `OPEN_SOURCE_RELEASE.md`, `RELEASE.md`, and the matching English documents.
- Maven POM files are no longer maintained as build entries.
- Public release notes should state that JDBM is fixed to `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5`, used together with the ApacheDS-side `JdbmTable` concurrency fix.
