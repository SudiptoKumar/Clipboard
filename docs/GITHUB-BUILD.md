# GitHub APK Build

This project is designed to build on GitHub Actions with a **manual-only** workflow.

## Workflow

Use:

`Actions -> Build LongPaste Keyboard APK -> Run workflow`

The workflow installs:

- Temurin JDK 17
- Android command-line tools through `android-actions/setup-android@v4`
- Android platform 36
- Android Build Tools 36.0.0
- Gradle 9.6.0

The deprecated Android SDK `tools` package is intentionally not requested.

## Outputs

Successful runs produce:

`app/build/outputs/apk/debug/app-debug.apk`

The APK is uploaded as the `LongPaste-debug-apk` artifact.

The workflow can optionally create a GitHub Release when `create_release` is enabled and a `release_tag` is supplied.

## Compatibility

The project uses Android Gradle Plugin 9.4.0, Gradle 9.6.0, JDK 17, Kotlin Gradle Plugin 2.2.10, and compile/target SDK 36.
