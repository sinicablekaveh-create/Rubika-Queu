# Reorganization proposal — reversible, no deletions

This recovered v1.6.3 source archive is the baseline from the checksum-verified PR #54 GitHub Actions artifact.

1. Materialize `app/src/main/java`, `app/src/main/assets`, `app/src/main/res` and AndroidManifest.xml in the **existing** GitHub project, on a feature branch. Keep the historical ZIPs, scripts, tests and docs.
2. Replace only conflicting sparse app classes/configuration from main with the recovered source. Keep old app package name and the original drawable icon. No old files are deleted by this patch.
3. Update CI to build checked-in source directly and run Gradle test/lint/assemble before uploading a real APK.
4. Restore the original `ir.sinicable.rubikaqueue` application ID, which differs from recovered baseline's `.triple` suffix; debug builds do not permit updating a differently signed installation.
5. Do not automatically upload/send image messages without a verified personal-account media transport. The editor accepts/previews/removes images but blocks unsupported sending rather than falsely reporting success.

No destructive changes were made to GitHub main. GitHub branch creation was rejected with HTTP 403 `Resource not accessible by integration`, despite connector reporting push permission.
