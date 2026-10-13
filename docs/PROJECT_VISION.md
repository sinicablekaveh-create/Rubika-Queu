# Rubika-Queu project vision

## Primary goal

Evolve Rubika-Queu into an intelligent, online, extensible Android application that supports AI-assisted development, safe code changes, tests, verified APK builds, releases, and continuous improvement **without compromising existing messaging and queue functions**.

## Target development sequence

Android application -> optional online AI -> Plugin Manager -> Capability Registry -> Workflow Engine -> authenticated GitHub/development integrations -> code analysis and proposed changes -> review -> GitHub Actions -> tests and Gradle -> signature-verified APK artifact -> approved release -> monitoring -> next cycle.

## Non-negotiable principles

1. **Independence:** The main application, message queue, and APK build must not depend on a particular optional AI model, research connector, plugin, or external development service.
2. **Online AI:** User-approved, authenticated AI operations are optional. Secrets remain on a trusted backend, not inside the APK.
3. **Scoped integrations:** GitHub, code review, research and remote computer services require independent credentials, narrow permissions, and explicit approvals for consequential changes.
4. **Real automation:** Only real commits, test output, Actions runs, verified APK artifacts and release records count as completion.
5. **Continuous development:** Each verified iteration may begin a new supervised development cycle.

## Current implementation boundaries

- The Android application and APK workflow predate this document.
- The dependency-free Java Plugin Manager, Capability Registry, and Workflow Engine are **foundational components**, not a fully deployed autonomous agent. UI integration, durable workflow state, asynchronous scheduling, provider adapters, authentication, approvals, release automation and monitoring remain to be implemented.
- Android source is extracted from an audited ZIP and modified by build overlays; these are essential inputs until a reviewed migration makes the source directly buildable.
- GitHub branch protection and signing secrets are repository configuration changes and cannot be inferred from source files.
- Never imply a successful APK or release without checking CI results and artifact evidence.

See DEPENDENCY_POLICY.md and ONLINE_AI_GATEWAY.md.
