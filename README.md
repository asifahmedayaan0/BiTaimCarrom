# BiTaimCarrom

Android project based on the supplied `BiTaimCarrom_Build.md` source.

- Package: `com.bitaim.carrom`
- minSdk: 29
- targetSdk: 34
- Build: `gradle :app:assembleDebug`

## GitHub Actions

The workflow at `.github/workflows/android-apk-release.yml` builds a debug APK
on manual dispatch and on version-tag pushes (`v*`). Manual runs publish the
APK as a downloadable workflow artifact. Pushing a tag such as `v1.0.0`
additionally creates a GitHub Release and attaches `BiTaimCarrom.apk`.

## Notes

This is an unsigned debug APK. It is suitable for testing; a distributable
signed release should use a signing key stored as GitHub Actions secrets.
