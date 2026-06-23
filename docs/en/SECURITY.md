# Security Policy

Language: [中文](../../SECURITY.md) | English

## Supported Versions

The current maintenance focus is the latest release and the `master` branch. When reporting an issue, include the affected version, commit hash, JDK, operating system, deployment mode, and storage backend whenever possible.

| Version | Status |
| --- | --- |
| `2.0.0-M27` | Supported |
| `master` | Supported |
| Earlier versions | Best effort |

## Private Vulnerability Reports

Do not report security vulnerabilities, exploit details, credentials, private keys, production DNs, access tokens, or identifiable user data through public issues, discussions, pull requests, or log attachments.

Use the GitHub Security Advisory private reporting entry:

<https://github.com/dibyang/apacheds/security/advisories/new>

If that entry is unavailable, contact the maintainers through an approved private channel and keep any public repository note free of exploit details.

## Report Contents

Security reports should include:

- Affected version, commit hash, and deployment mode.
- JDK, operating system, network exposure, and storage backend.
- Vulnerability type, impact, and whether remote exploitation is possible.
- Minimal reproduction steps or a safe proof of concept.
- Logs, stack traces, configuration snippets, and mitigation suggestions.

Redact passwords, tokens, certificate private keys, real user data, and production-specific information before sending the report.

## Handling Process

Maintainers will first confirm impact, reproduction conditions, and temporary mitigations. Exploit details are not disclosed publicly before a fix is ready. Issues involving dependencies, ApacheDS upstream, or JDBM will be coordinated with the relevant project when necessary.

## Non-security Issues

Use the GitHub bug template for ordinary bugs, performance issues, compatibility issues, and documentation problems. If you are unsure whether an issue is security-sensitive, report it privately first.
