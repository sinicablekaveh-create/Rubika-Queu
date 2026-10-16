# مسیر امضای ثابت برای «صف روبیکا • نسخه دوم»

**هدف:** اضافه‌کردن امکان انتشار نسخه‌های آتی با گواهی امضای ثابت به
`ir.sinicable.rubikaqueue.sidecar`، مستقل از برنامه اصلی با شناسه
`ir.sinicable.rubikaqueue`.

## وضعیت نسخه نصب‌شده اکنون

نسخه دوم موجود روی گوشی از یک کلید آزمایشی موقت CI استفاده کرده است.
**APK امضاشده با کلید دائمی جدید نمی‌تواند همان نصب فعلی را درجا آپدیت کند.**
بدون حذف نصب و بدون داشتن کلید همان APK، راهی برای تعویض امضا با حفظ
اطلاعات خصوصی برنامه توسط Android وجود ندارد. صف فعلی را پاک نکنید.
گزارش CSV نیز لزوماً نسخه پشتیبان قابل‌بازیابی از تمام داده‌های صف نیست.
این PR هیچ اطلاعاتی را از گوشی نمی‌خواند و هیچ نسخه‌ای را نصب نمی‌کند.

## معماری و آزمون

- Build type جدا به نام `sidecarStable` با
  `applicationId=ir.sinicable.rubikaqueue.sidecar`، نام و آیکون متمایز.
- نسخه‌های `debug` همچنان از کلید آزمایشی استفاده می‌کنند و برای
  آپدیت درجا توصیه نمی‌شوند؛ `release` اصلی بدون تغییر باقی است.
- `sidecarStable` فقط با کلید اختصاصی برنامه دوم امضا می‌شود:
  `RUBIKA_SIDECAR_*`، نه کلید برنامه اصلی یا debug.
- در هر اجرای CI معمولی، ساخت واقعی این نوع APK با **کلید یک‌بارمصرف**
  آزمایش و خروجی پاک می‌شود؛ نه فایل کلید و نه APK موقت انتشار نمی‌یابد.
- انتشار با کلید ثابت فقط از branch `main`، به‌صورت دستی، با انتخاب
  `sidecar_stable` در workflow و فقط پس از صحت‌سنجی شناسه، گواهی،
  غیرقابل‌دیباگ‌بودن و امضا امکان‌پذیر است. **این دستور انتشار خودکار نیست.**

## آماده‌سازی کلید ثابت روی دستگاه مورداعتماد مالک

کلید جدید را **یک‌بار** روی رایانه شخصی امن ایجاد کنید و دو نسخه پشتیبان
آفلاین از کلید و گذرواژه نگه دارید. فایل `.p12` را در چت، فایل‌های
ریپازیتوری یا PR قرار ندهید. نمونه با JDK و PowerShell در یک پوشه
خارج از Git:

```powershell
keytool -genkeypair -alias rubika-sidecar-stable -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000 -storetype PKCS12 -keystore rubika-sidecar-stable.p12 -dname "CN=Rubika Queue Sidecar, OU=Android, O=Owner, C=IR"
keytool -list -v -storetype PKCS12 -keystore rubika-sidecar-stable.p12 -alias rubika-sidecar-stable
```

اثر انگشت SHA-256 عمومی را بدون دونقطه و با حروف کوچک در GitHub Actions
**repository variable** زیر ثبت کنید:
`RUBIKA_SIDECAR_SIGNER_SHA256`.

چهار **repository secret** جداگانه لازم است:
`RUBIKA_SIDECAR_KEYSTORE_BASE64`،
`RUBIKA_SIDECAR_KEYSTORE_PASSWORD`،
`RUBIKA_SIDECAR_KEY_PASSWORD`،
`RUBIKA_SIDECAR_KEY_ALIAS`.
مقدار secret اول Base64 تمام فایل p12 است و باید فقط مستقیم در بخش
Secrets GitHub ثبت شود. برای تولید محلی در PowerShell:
`[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path '.\\rubika-sidecar-stable.p12')))`.
آن را در گفتگو یا خروجی عمومی چاپ نکنید.

پس از بررسی مسیر انتقال واقعی داده‌ها و رضایت صاحب حساب، اجرای
`Build Rubika Queu Test APK` با انتخاب `main → sidecar_stable`
یک APK تازه برای **نصب اولِ مسیر امضای ثابت** می‌سازد. این APK هنوز
نمی‌تواند نسخه نصب‌شده CI را آپدیت کند. برای نسخه‌های آینده باید همان
کلید و fingerprint استفاده شود و `versionCode` افزایش یابد.
چیزی را حذف نکنید تا انتقال داده به صورت مستقل اثبات شود.
