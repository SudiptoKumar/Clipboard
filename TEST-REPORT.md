# LongPaste Keyboard v1.0.2 Test Report

## Root cause fixed

The previous GitHub Actions run reached Gradle but failed before compilation with:

`Cannot add extension with name 'kotlin', as there is an extension already registered with that name.`

AGP 9.x provides built-in Kotlin support. v1.0.2 therefore removes the standalone `org.jetbrains.kotlin.android` plugin from both the root and module build files and removes the deprecated `android.kotlinOptions` configuration.

## Source-level validation

- 1 MiB ASCII reconstruction: PASS
- Unicode reconstruction including Bangla, emoji, accented Latin, and CJK: PASS
- Chunk-count boundary tests: PASS
- Kotlin utility compilation/execution: PASS
- XML/resource structure: PASS
- Workflow YAML structure: PASS
- Manual-only `workflow_dispatch` trigger: PASS
- No Internet permission in AndroidManifest: PASS
- No `org.jetbrains.kotlin.android` plugin in live Gradle files: PASS
- No `android.kotlinOptions` DSL in live Gradle files: PASS

## GitHub Actions build path

The workflow uses `android-actions/setup-android@v4`, requests `platform-tools`, and installs the required `platforms;android-36` and `build-tools;36.0.0` packages explicitly. It then runs Gradle configuration validation, unit tests, APK assembly, and an APK existence check.

## Build status

The actual Android APK build must be executed on GitHub Actions because the local environment used to prepare this ZIP does not contain the Android SDK or a Gradle installation. This ZIP is therefore not labeled as locally APK-build-verified.
