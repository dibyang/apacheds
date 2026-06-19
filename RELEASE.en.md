# Release Runbook

Language: [中文](RELEASE.md) | English

This runbook is required for formal releases. The default flow is upload to Sonatype Central Portal for manual review, not automatic publication to Maven Central.

## 1. Release Principles

- Release actions must target the current repository checkout.
- Maven POM files have been removed. Gradle Wrapper is the only supported build and publication entry.
- Project version, internal dependency versions, and public POM metadata are configured in `gradle.properties`.
- Secrets must stay local in `signing.properties`, Gradle user properties, or environment variables.
- Central publication must first upload to Sonatype Central Portal for manual review.
- Do not publish/release automatically and do not use `publishing_type=automatic` without explicit maintainer approval.
- If a version is partially published to Maven Central, stop using that version and restart the full release flow with a new version.

## 2. Version Preparation

Update `gradle.properties`:

```properties
projectVersion=2.0.0-M26
jdbmVersion=2.0.0-M5
```

Before release, verify:

- The fallback version in `build.gradle` matches `projectVersion`.
- [CHANGELOG.en.md](CHANGELOG.en.md) documents user-facing changes.
- [OPEN_SOURCE_RELEASE.en.md](OPEN_SOURCE_RELEASE.en.md) records validation results, known risks, and release notes.
- This runbook reflects the latest release process.
- No obsolete `pom.xml` files remain.

## 3. Local Checks

Check for leftover Maven POM files:

```powershell
Get-ChildItem -Recurse -Filter pom.xml
```

Review the working tree:

```powershell
git status --short
git diff --name-only
```

Validate release configuration:

```powershell
.\gradlew.bat validateReleaseConfiguration
```

Confirm that JDBM resolves to the fixed version:

```powershell
.\gradlew.bat --refresh-dependencies :jdbm-partition:dependencyInsight --dependency apacheds-jdbm1 --configuration compileClasspath
```

Run the DIRSERVER-2102 concurrency suite:

```powershell
.\gradlew.bat :jdbm-partition:jdbmConcurrencyTest --rerun-tasks
```

Run the demotion/valueCursor stress case:

```powershell
.\gradlew.bat --% :jdbm-partition:jdbmConcurrencyTest --rerun-tasks --tests org.apache.directory.server.core.partition.impl.btree.jdbm.DIRSERVER2102JdbmConcurrencyTest.testConcurrentBTreeDemotionAndValueCursorDoNotReadDeletedBTree -Ddirserver2102.threadCount=24 -Ddirserver2102.iterations=600
```

Assemble all modules:

```powershell
.\gradlew.bat assemble
```

Verify local Maven artifacts, POMs, sources, javadocs, and signatures:

```powershell
.\gradlew.bat publishToMavenLocal
```

If time allows, run the full build in a terminal without a short timeout:

```powershell
.\gradlew.bat build
```

## 4. Upload to Central Portal for Review

Before upload, check that no stale publication process is running:

```powershell
Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match 'gradle|publishAllPublications|SonatypeCentral|central.sonatype' } | Select-Object ProcessId,Name,CommandLine
```

Upload all module publications to the Sonatype Central staging repository:

```powershell
.\gradlew.bat publishAllPublicationsToSonatypeCentralRepository --console=plain
```

After upload, record:

- repository key
- deployment id
- Portal state
- errors
- warnings
- artifact versions in purls

## 5. Manual Review Gate

After upload, the deployment must stay in the review state. The default flow is Central Portal user-managed publication.

At minimum, manual review must verify:

- Portal deployment state is `VALIDATED`.
- `errors` is empty.
- All `purls` use the current version, for example `2.0.0-M26`.
- No abandoned version such as `2.0.0-M25` is mixed in.
- Key modules exist, for example `apacheds-jdbm-partition`, `apacheds-ldif-partition`, `apacheds-core-api`, `apacheds-service`, and `apacheds-all`.
- Internal ApacheDS dependency versions are consistent in POMs.
- `net.xdob.directory.jdbm:apacheds-jdbm1` is fixed to `2.0.0-M5` in POMs.
- sources jars, javadoc jars, and `.asc` signatures are present.
- `LICENSE`, `NOTICE`, `DEPENDENCIES`, and POM license/scm/developer metadata are reasonable.

Do not perform final publish/release before human approval.

## 6. Final Publication

Only publish/release after the maintainer explicitly says that manual review has passed and the deployment may be published to Maven Central.

If the context is unclear, confirm:

- Is this only upload-for-review, or final publication to Maven Central?
- Is skipping manual review allowed?
- Is the current deployment id confirmed?

## 7. Failure Handling

If upload or release is interrupted:

1. Check background Gradle/Java processes immediately.
2. If upload is still running, record PID and command line and do not rerun the publication command.
3. Query the current Central Portal repository/deployment state.
4. Check whether any modules were partially published.

If a version is partially published to Maven Central:

1. Stop using that version.
2. Record published and unpublished coordinates.
3. Do not overwrite, delete, or complete the same version.
4. Move to a new version and rerun the full release flow.

If an old open/closed staging repository exists in Central Portal:

1. Confirm that it belongs to an abandoned version or failed upload.
2. Drop it only after maintainer confirmation.
3. Query again and confirm that the list is clear or the state is expected.
