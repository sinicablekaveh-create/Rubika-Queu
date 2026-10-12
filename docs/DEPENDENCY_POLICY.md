# Build and optional-dependency policy

## Build-critical inputs (keep)

- Java 17, Android SDK platform 35 and build-tools 35.0.0, Gradle 8.9.
- The v1.1 audited Android source ZIP and its patches.
- Existing Python source overlay installers and Java overrides.
- Node and Java tests, Android lint, APK signature and zipalign verification.
- The build-apk.yml and verify-test-apk.yml workflows.
- A persistent test signing key **only** when an APK must upgrade a previously installed APK signed with that key. Debug APK builds do not need this key, but are NOT update-compatible.

## Optional inputs (never gate ordinary APK builds)

- Online AI model provider, research tools, CodeRabbit, and other remote plugins.
- User-owned HTTPS development gateway and its credentials.
- The independent Java plugin core under overrides/plugin-system has no Android, Clojure, Conda, or third-party SDK dependencies.
- Clojure/Leiningen is not used: there is no project.clj or Clojure module.
- Conda is not used: there is no environment.yml. Python 3 itself **is** required by the existing source overlay scripts.
- Maven/Gradle package publication is not Android APK release.

## CI simplification

Keep Android APK CI and separate core Java/Node tests. Remove unrelated templates: Clojure, Conda, Hello World, outdated Java-11 Android build, and Gradle package publication. Remove the nonessential dependency-submission job that assumes a directly buildable root Gradle project.

The root repository stores audited Android source in a ZIP and uses overlays at CI runtime. Removing this ZIP without migrating the Java sources would break the build; it is a **remaining structural dependency**.

## Windows verification without hosted CI

Run `powershell -NoProfile -ExecutionPolicy Bypass -File .\\scripts\\verify-plugin-core.ps1`
from the repository root after reviewing the script. This requires only a JDK
(`java` and `javac` available in PATH), and verifies the pure-Java plugin
core without Android SDK, Clojure, Conda, online AI, or GitHub Actions.

This is **not** a replacement for the complete Android lint, Gradle APK
build, signature verification, and APK artifact checks; those still must pass
before merge or release.

## Runtime failure policy

- If a provider is unavailable or raises an exception, CapabilityRegistry attempts the next provider by priority.
- PluginManager registers providers on enable, unregisters them on disable, and rolls back partial registration on error.
- WorkflowEngine runs synchronous steps with bounded attempts and explicit success/failure/cancellation. Persistent resume, time-based backoff, crash recovery, sandboxing and automatic deployment **are not implemented**.

Require scoped permissions, review and PR checks for code-changing operations. Never expose provider or GitHub tokens in Android source, workflow logs, or APK resources.
