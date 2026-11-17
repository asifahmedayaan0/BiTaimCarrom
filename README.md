# BiTaimCarrom

Android application source for BiTaim Carrom.

- Package: `com.bitaim.carrom`
- Module: `app`
- Minimum SDK: 29
- Target/compile SDK: 34
- Android Gradle Plugin: 8.4.2
- Kotlin Gradle plugin: 1.9.24
- Gradle: 8.6
- JDK: 17

## Build

Run `gradle :app:assembleDebug`. The unsigned debug APK is written to
`app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

Run the **Android APK** workflow manually to build and download the APK artifact.
Pushing a version tag such as `v1.0.0` also creates a GitHub Release and attaches
`BiTaimCarrom.apk`. The APK produced by this workflow is an unsigned debug build.
