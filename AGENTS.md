# Source and safety instructions

- Edit the checked-in `app/src/main` source from the recovered v1.6.3 archive; never substitute v1.1 extracted CI source.
- Keep package ID `ir.sinicable.rubikaqueue`, original `@drawable/icon`, and legacy slot 1 vault and job filenames.
- Require explicit recipient consent and respect rate limits. Treat in-flight send interruption as uncertain; never automatically retry a possibly delivered message.
- No authenticated personal-account image upload is implemented in RubikaClient. Do not use Bot API or undocumented endpoints to simulate it; block photo queue sends until verified provider transport and tests exist.
- Never commit private keys, account sessions, user data, signed URLs or temporary keystores.
- Build with Java 17, Gradle 8.9 and Android SDK platform/build-tools 35. Use the included build-verified workflow for an actual APK.
- Do not delete historical assets, archives or app classes without separately reviewing proposed deletions.
