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
