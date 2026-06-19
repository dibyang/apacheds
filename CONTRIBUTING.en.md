# Contributing Guide

Language: [中文](CONTRIBUTING.md) | English

## Basic Requirements

- Chinese is the default language for commit messages, documentation, and important comments. English is maintained as separate secondary documentation.
- Text documents must use UTF-8 encoding.
- Use Gradle Wrapper as the build entry. Maven POM files are no longer build entries.
- Changes to release, build, dependency, or persistence logic must update related documentation.

## Development Checks

Before committing, at least run:

```powershell
git status --short
git diff --check
```

For Java code changes, run the smallest relevant test or compile task. For JDBM, BTree, cursor, partition, or DIRSERVER-2102 changes, run:

```powershell
.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest
```

## Release-related Changes

For version, publication configuration, Sonatype Central, signing, POM metadata, or release documentation changes, check:

- [RELEASE.en.md](RELEASE.en.md)
- [OPEN_SOURCE_RELEASE.en.md](OPEN_SOURCE_RELEASE.en.md)
- [CHANGELOG.en.md](CHANGELOG.en.md)
- `gradle.properties`

Central publication defaults to upload-for-manual-review only. Do not publish automatically to Maven Central without explicit approval.

## Documentation Rules

- Chinese documents are the default entry points.
- English documents use matching `.en.md` files.
- Chinese documents should link to the English version at the top, and English documents should link back to the Chinese version.
- Do not scatter secondary-language paragraphs inside the primary document. Maintain separate language versions when bilingual documentation is needed.
