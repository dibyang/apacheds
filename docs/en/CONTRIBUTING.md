# Contributing Guide

Language: [中文](../../CONTRIBUTING.md) | English

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

- [RELEASE.md](RELEASE.md)
- [OPEN_SOURCE_RELEASE.md](OPEN_SOURCE_RELEASE.md)
- [CHANGELOG.md](CHANGELOG.md)
- `gradle.properties`

Central publication defaults to upload-for-manual-review only. Do not publish automatically to Maven Central without explicit approval.

## Documentation Rules

- Chinese documents are the default entry points.
- English copies live under `docs/en/` instead of beside the Chinese root documents.
- Chinese documents should link to the English copy under `docs/en/`, and English documents should link back to the Chinese root document.
- Do not scatter secondary-language paragraphs inside the primary document. Maintain separate language versions when bilingual documentation is needed.
