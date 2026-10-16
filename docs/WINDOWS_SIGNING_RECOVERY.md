# Windows APK signing recovery (read-only first)

Goal: recover a matching signing key for the owner's reported installed Rubika-Queu v1.3.2 (versionCode 15), then verify whether a safe in-place update is possible without deleting the local queue or history.

## Ground truth

- Application ID: ir.sinicable.rubikaqueue
- The owner's installed v1.3.2 uses versionCode 15; current `main` candidate is versionCode 16 (2026-10-16). Any proposed update must increase versionCode beyond the installed version.
- Required installed signer certificate SHA-256: `fdb827fa66b05309a2e7a759bc03d9f22355b54cd74c63e74a5d889823087026`.
- The verified dedicated v0.8/v1.0/v1.1 test APK signer fingerprint is:
  8747838d913e2f0f58dc66e432eb3753bc826e6a3f7e23eaf6bb799cd63af28b.
- GitHub Actions builds with ephemeral debug or legacy test keys are **NOT-UPDATABLE** against the owner's installed certificate, even if CI succeeds. The existing `main` build publishes artifacts with an update-compatibility classification, not proof of an on-device upgrade.
- A certificate fingerprint or a signed APK cannot be used to reconstruct the private signing key.
- Do not uninstall or clear application data. The Android manifest currently sets android:allowBackup="false".

## Phone-only preparation (Windows offline)

Development and CI can proceed in GitHub without the Windows computer. To identify the installed app's **public signing certificate** before a safe update, an owner may export the APK of the **currently installed** `ir.sinicable.rubikaqueue` package with a trusted local Android package-export tool (only if their device allows this), then store it privately, for example in their own Google Drive. Keep the export out of public repositories, public PR attachments, and public sharing links. Do not export app data or login credentials. A downloaded GitHub APK is not evidence of the signature on the phone.

An authorized cloud environment with the official Android SDK Build Tools may inspect that supplied APK read-only:

~~~text
apksigner verify --print-certs <exported-installed-base.apk>
~~~

Check the APK's package name and versionCode with Android SDK metadata tooling as a separate step. If the device uses split APKs, identify the actual installed base APK; an arbitrary downloaded split/package does not prove the installed app's identity. Only trust the fingerprint when the file's provenance from the installed application is verified. Exporting an APK cannot provide the private signing key required to build an update.

Once Windows is available, the PowerShell script also accepts an already-exported file without ADB:

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\inspect-windows-apk-signing.ps1 -ApkPath 'C:\Users\ABM\Downloads\installed-base.apk'
~~~

The `-ApkPath` mode does not modify or delete the supplied APK, does not use ADB, and does not establish that the file matches the installed app unless provenance was independently verified.

## 1. Reconnect Windows

The Windows device must be online in Remote Desktop Commander before a remote shell can run. Start its Remote agent from Windows. For a Node-based installation check Node first:

~~~powershell
node --version
npm --version
npx --version
~~~

If Node/npm/npx are unavailable, install a supported Node.js LTS release from the official source and open a new PowerShell window. Run the Remote Desktop Commander command specified by its own onboarding instructions, such as:

~~~powershell
npx @wonderwhy-er/desktop-commander@latest remote
~~~

Connection and device permissions must be completed by the owner.

## 2. Read-only signing inspection with a USB-connected Android phone

Enable Developer options and USB debugging temporarily, connect the phone, and authorize the displayed ADB prompt. Use the checked-in diagnostic script on Windows:

~~~powershell
cd C:\Users\ABM\AndroidStudioProjects\Rubika-Queu
git fetch origin diagnostics/windows-apk-signing-identity
git switch --track origin/diagnostics/windows-apk-signing-identity
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\inspect-windows-apk-signing.ps1
~~~

If this folder is not a Git checkout, instead clone this diagnostic branch into a new separate folder; do not overwrite an existing Android Studio project:

~~~powershell
cd C:\Users\ABM\AndroidStudioProjects
git clone --branch diagnostics/windows-apk-signing-identity https://github.com/sinicablekaveh-create/Rubika-Queu.git Rubika-Queu-signing-audit
cd .\Rubika-Queu-signing-audit
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\inspect-windows-apk-signing.ps1
~~~

The script enumerates keystore file paths only, checks the ordinary Android Studio debug certificate using its documented default password, and, when ADB and Android SDK Build Tools are available, copies only the installed APK temporarily to %TEMP% to verify the public signing certificate. It removes that temporary APK afterward. It never reads or uploads private keys, passwords, or application data.

## 3. Compare identity

Compare these fingerprints:

1. Installed v1.3.2/code15 APK: Signer #1 certificate SHA-256 digest printed by apksigner; expected value is `fdb827fa66b05309a2e7a759bc03d9f22355b54cd74c63e74a5d889823087026`.
2. Default Windows Android Studio debug key, usually %USERPROFILE%\.android\debug.keystore, as printed by keytool.
3. Dedicated test key, if an original test-signing.p12 file is located locally. To check a non-default PKCS12 file, run keytool -list -v -storetype PKCS12 -keystore <local-path> locally and enter the password only at the interactive prompt, not in chat or scripts.
4. Dedicated GitHub certificate from v1.1 above.

If the installed APK certificate matches a keystore's certificate and that keystore contains the signing private key, that key is a candidate for an updatable build. If it matches only a GitHub Actions per-run debug certificate, the corresponding private key might have been destroyed with its ephemeral runner; admin permissions cannot recover it.

## 4. Next build gate, only after a matching key is established

- Preserve applicationId, use a higher versionCode than the installed APK, sign using the verified matching private key.
- Run apksigner verify --print-certs against the produced APK and compare it with the installed APK's signer.
- Perform a controlled update test on the actual device, without adb uninstall, pm clear, -d downgrade flags, or wiping user data.
- Check queue history, local storage and login state afterward.
- Only then configure narrowly scoped GitHub Actions secrets for the verified key; do not store the private key in the public repository or send passwords through chat.

This guide deliberately does not migrate the GitHub workflow to a new signing key or trigger release publication before the certificate match is verified.
