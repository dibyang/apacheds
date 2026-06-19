# ApacheDS Gradle Release Branch

Language: [中文](../../README.md) | English

This repository is a Gradle build and publication branch for Apache Directory Server. The current release version is `2.0.0-M26`. This branch fixes JDBM to `net.xdob.directory.jdbm:apacheds-jdbm1:2.0.0-M5` and keeps DIRSERVER-2102 concurrency regression coverage.

## Documentation

- [CHANGELOG.md](CHANGELOG.md): version changelog.
- [OPEN_SOURCE_RELEASE.md](OPEN_SOURCE_RELEASE.md): release notes for this open source publication.
- [RELEASE.md](RELEASE.md): formal release runbook.
- [CONTRIBUTING.md](CONTRIBUTING.md): contribution and validation requirements.
- `LICENSE`, `NOTICE`, `DEPENDENCIES`: license, notice, and dependency information.

## Requirements

- JDK 8
- Gradle Wrapper
- Windows PowerShell or an equivalent shell

## Common Commands

Build:

```powershell
.\gradlew.bat build
```

Assemble:

```powershell
.\gradlew.bat assemble
```

Validate release configuration:

```powershell
.\gradlew.bat validateReleaseConfiguration
```

Verify local Maven publication:

```powershell
.\gradlew.bat publishToMavenLocal
```

Run the DIRSERVER-2102 concurrency suite:

```powershell
.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest
```

## Release Rules

Central publication must follow [RELEASE.md](RELEASE.md). The default flow only uploads to Sonatype Central Portal for manual review. Do not publish automatically to Maven Central without explicit maintainer approval.

## License

This project is licensed under the Apache License 2.0. See [LICENSE](../../LICENSE).
