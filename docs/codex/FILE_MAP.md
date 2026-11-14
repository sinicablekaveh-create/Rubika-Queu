# File Map for Codex

## Core

- MultiAccountActivity.java — three-account UI, imports, consent, controls
- RubikaClient.java — login, API calls, recipient resolution, text send
- CryptoCore.java — protocol cryptographic helpers
- Vault.java — encrypted account sessions
- JobStore.java — queue persistence
- QueueService.java — slot 1
- QueueService2.java — slot 2
- QueueService3.java — slot 3
- MessageQueuePolicy.java — queue policy
- QueueDuplicateGuard.java — duplicate suppression
- RetryBackoff.java — retry timing
- SendInterval.java — interval validation
- SendResponse.java — message-ID extraction
- SentHistoryStore.java — local send history
- AccountSlots.java — slot/service mapping
- OperationGate.java — operation coordination
- ExcelReader.java — XLSX/CSV import
- Phones.java — Iranian mobile normalization
- NetworkState.java — connectivity checks

## Native

- app/src/main/cpp/native_send_core.cpp
- app/src/main/cpp/CMakeLists.txt
- NativeSendCore.java

## Android/build

- app/src/main/AndroidManifest.xml
- app/src/main/res/xml/network_security_config.xml
- app/build.gradle
- build-fixed-apk.sh
- scripts/package_v3_release.py

## Continuation documentation

- CODEX_CONTINUATION_PROMPT.md
- README-CODEX.md
- AGENTS.md
- docs/codex/*.md
