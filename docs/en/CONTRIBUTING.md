# Contributing Guide

Language: [中文](../../CONTRIBUTING.md) | English

## Basic Requirements

- Chinese is the default language for commit messages, documentation, and important comments. English is maintained as separate secondary documentation.
- Text documents must use UTF-8 encoding.
- Use Gradle Wrapper as the build entry. Maven POM files are no longer build entries.
- Changes to release, build, dependency, or persistence logic must update related documentation.

## Feedback Entry Points

- Use the GitHub bug template for ordinary bugs, regressions, performance issues, and compatibility issues.
- Bug reports should include version, JDK, operating system, storage backend, reproduction steps, actual behavior, expected behavior, and relevant logs.
- Do not publicly report issues involving passwords, tokens, private keys, production DNs, real user data, or exploit details. Follow [SECURITY.md](SECURITY.md) for private reporting.
- If you are unsure whether an issue is security-sensitive, use the security reporting path first.

## Contribution Flow

1. Search existing issues and changes first to avoid duplicates.
2. For behavior, compatibility, persistence, or concurrency changes, describe the approach, risks, and rollback path in the issue or pull request.
3. Keep pull requests focused. One pull request should address one issue or a tightly related set of issues.
4. Pull request descriptions should include change summary, validation commands, compatibility impact, and remaining risk.
5. Do not commit local credentials, signing files, build caches, production logs, or incompletely redacted data.

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

For Markdown-only, GitHub issue-template, or security-policy changes, at least run:

```powershell
git diff --check
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
