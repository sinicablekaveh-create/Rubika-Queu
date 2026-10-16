## ChatGPT development report (v1.3.2, code 15)

The main queue screen now offers **گزارش توسعه برای ChatGPT**: an explicit development-request form, locally generated public app metadata (package/version/Android SDK), an editable preview, copy, Android text-sharing chooser, and an opt-in ChatGPT website handoff. The app does not automatically read contacts, queued messages, private Rubika data, tokens, or credentials. No ChatGPT session or API integration is implied, and no outgoing Rubika message is sent by this flow.

**Upgrade caveat:** This source version raises code 14 to code 15, retaining package `ir.sinicable.rubikaqueue`. The owner's installed v1.3.1/code14 certificate is `fdb827fa66b05309a2e7a759bc03d9f22355b54cd74c63e74a5d889823087026`, which **does not match** the test certificate currently documented in CI. The build workflow reports the candidate's certificate and will mark it `NOT_UPDATABLE` unless the original installed certificate's signing key is supplied through a secure GitHub Actions secret. Do not uninstall the installed app or assume a new debug APK preserves data. CI does not recover the signing private key from the installed APK.

# Rubika-Queu – Android Message Queue

[![Build Android APK](https://github.com/sinicablekaveh-create/Rubika-Queu/actions/workflows/build-apk.yml/badge.svg)](https://github.com/sinicablekaveh-create/Rubika-Queu/actions/workflows/build-apk.yml)

This Android project contains a private-account queue and independent optional integration with the **publicly documented Rubika Bot API**.

## Official Rubika integration (v0.9)
The main screen includes a button **وب‌سایت رسمی و API بات روبیکا** that opens the optional, non-exported OfficialBotActivity. It can open official web pages in the device's normal browser:
- https://rubika.ir/home
- https://web.rubika.ir/
- https://rubika.ir/botapi

The independent `OfficialBotApi` client implements documented HTTPS POST requests:
- `https://botapi.rubika.ir/v3/{token}/getMe` – identify the supplied bot
- `https://botapi.rubika.ir/v3/{token}/sendMessage` – user-confirmed single test message to an allowed chat

Get your **own bot token** from BotFather. The bot token is kept only in the transient input field and is never committed, logged to CI, or written into the existing queue. One test message requires explicit confirmation and a 30-second local cooldown. No unattended bulk sending via the Bot API is implemented. The existing personal-account queue and its retry logic remain unchanged.

The **public site** does not publish the backend or source of the private Rubika Android app. No proprietary source, bundled website JavaScript, or undocumented browser endpoints are copied into the APK. The public website's URLs and Bot API documentation are used as integration references.

## Build and signing
The complete source bundle is archived as `source/Rubika-Queu-v0.9-official-web-integration-source.zip` and extracted by `.github/workflows/build-apk.yml`. The main branch's manifest and Gradle configuration govern the resulting build. GitHub Actions runs Android Gradle, `apksigner verify`, `zipalign`, and uploads a built APK as an Actions artifact.

Without a custom signing secret, GitHub Actions uses a fresh runner debug keystore: the resulting artifact has a **different signing identity** and **cannot upgrade** an already installed Rubika-Queu APK signed with the permanent test certificate. For a safely updatable APK, configure GitHub Actions secret `RUBIKA_TEST_KEYSTORE_BASE64` with the original PKCS#12 keystore (alias `test`) and retain its passwords via `RUBIKA_TEST_KEYSTORE_PASSWORD` and `RUBIKA_TEST_KEY_PASSWORD`. Never commit a signing keystore or token. The workflow checks the expected certificate SHA-256 digest when the original key is configured.

Application ID: `ir.sinicable.rubikaqueue`, minimum SDK 26, target SDK 34, versionCode 9.

Official references:
- https://rubika.ir/botapi
- https://rubika.ir/botapi/methods

## Official Rubika 4.1.1 handoff

The connected Drive library contains the separately installable official
`Rubika_4.1.1_411.apk` (package `app.rbmain.a`, version code 411). The queue app
does not embed or decompile that APK. Its build adds an explicit Android bridge
that launches the installed official client and shares a user-approved text
draft for review. The handoff never reads the official app's private data and
never sends a message automatically. See
[`docs/OFFICIAL_RUBIKA_4_1_1_BRIDGE.md`](docs/OFFICIAL_RUBIKA_4_1_1_BRIDGE.md)
for the artifact fingerprint and device-test requirement.


## v1.0: زمان‌بندی و تاریخچهٔ ارسال
- انتخاب تاریخ و ساعت محلی از بخش فایل و متن برای شروع خودکار صف، به‌همراه امکان زمان‌بندی مجدد.
- درخواست مجوز SCHEDULE_EXACT_ALARM برای شروع دقیق در پس‌زمینه؛ بازیابی زمان‌بندی بعد از راه‌اندازی مجدد.
- آرشیو مستقل SQLite برای پیام‌های ثبت‌شده و امکان مشاهدهٔ فهرست، زمان، مخاطب، متن، شناسه پیام، و نوع تأیید؛ دریافت/خواندن پیام از روی ثبت سرور استنباط نمی‌شود.
- گسترش CSV با sent_at / reviewed_at / scheduled_start_at.


## v1.1: File & Text → Personal Account Queue Sender

The send controls remain in the main **فایل و متن پیام** page and use the locally encrypted personal-account session; Bot API stays independent. A per-queue 30–3600 second interval now controls the gap between successive message attempts and is saved in JobStore. Legacy jobs default to 30 seconds. The UI supports setting/changing this interval on queue creation and resume, while the older future-start scheduler stays optional. Live counts and SQLite receipt history distinguish server acknowledgement from actual delivery. Rate errors still trigger backoff and do not bypass provider limits. Use only with recipients who expect and have agreed to messages.

The complete patched Android sources, plus isolated Java interval tests, are in `source/Rubika-Queu-v1.1-personal-queue-source.zip`; the build workflow extracts these before compilation. App versionCode is 11.

## Rate-limit retry reliability fix

Explicit rate rejections now increase the retry delay beyond the old fifth-retry plateau: 10, 20, 40, 80, 160, 320, and 640 seconds, then a maximum of 900 seconds. Random jitter is included within that ceiling. Persisted retry counts and jitter are bounded to avoid overflow. The existing send interval and manual review of ambiguous results still apply. The bundled `RetryBackoffTests` run before the APK build in GitHub Actions.

## Automatic topic-based group discovery (opt-in)

The **کشف هوشمند گروه‌ها** screen accepts comma-separated Persian topics, for example `برق صنعتی، سینی کابل، نردبان کابل`. One explicit search automatically looks up up to eight focused topics through the existing discovery engine, ranks real results, and caches identified groups per signed-in account. The optional HTTPS Ollama integration only expands search phrases with explicit confirmation; it is not required for basic multi-topic search. A running search and its progress survive screen rotation. Group links and GUIDs are reconciled in the local cache. No automatic group joining, contact extraction, or messages are performed. See [AI discovery](docs/ai-discovery.md).

## Optional online AI inside the app

The drafting assistant can now call a **user-owned authenticated HTTPS gateway**
for cloud model suggestions and show replies directly inside Android, while the
existing browser handoff and Ollama modes remain available. No ChatGPT web
session, saved conversations, or ChatGPT plugins are imported into the APK.
Provider API keys belong **only on the server**; a short-lived gateway token is
entered in the UI and is not saved by the app. The draft is sent only after
confirmation, and AI never sends Rubika messages or updates code automatically.
See [Online AI gateway](docs/ONLINE_AI_GATEWAY.md) for deployment and testing.
For an owner-operated Windows HTTPS server, see
[Personal gateway deployment](docs/PERSONAL_GATEWAY_DEPLOYMENT.md).
